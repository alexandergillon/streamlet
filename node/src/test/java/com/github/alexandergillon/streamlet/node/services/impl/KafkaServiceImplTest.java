package com.github.alexandergillon.streamlet.node.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.ProposeMessage;
import com.github.alexandergillon.streamlet.node.models.VoteMessage;
import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import com.github.alexandergillon.streamlet.node.services.KafkaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// NOTE: need to precompute all digital signatures as ECDSA signatures are different each time. I.e. compute once and
// use everywhere: in mocks, in assertions, etc. (as opposed to computing every time with sign() or signBase64())
@SpringBootTest
class KafkaServiceImplTest {

    @Value("${streamlet.testing.keystore.private.directory}")
    private String privateKeyDirectory;
    @Value("${streamlet.testing.keystore.private.password}")
    private String keystorePassword;

    @MockBean
    private KafkaService mockKafkaService; // not for testing - just to allow spring context to come up so properties get injected

    @Mock
    private BlockchainService blockchainService;

    @Mock
    private CryptographyService cryptographyService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    CompletableFuture<SendResult<String, String>> result;

    @InjectMocks
    private KafkaServiceImpl kafkaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final int nodeId = 4;

    @BeforeEach
    public void injectProperties() {
        ReflectionTestUtils.setField(kafkaService, "nodeId", nodeId);
        ReflectionTestUtils.setField(kafkaService, "broadcastTopicName", "topicName");
    }

    // Tests rejected proposals are processed correctly
    @Test
    public void testRejectedProposal() {
        int proposer = 3;
        Block proposedBlock = TestUtils.getRandomBlock();
        byte[] signature = TestUtils.randomPayload();
        ProposeMessage proposeMessage = new ProposeMessage(proposer, proposedBlock.toJsonBlock(), Base64.getEncoder().encodeToString(signature));

        when(blockchainService.processProposedBlock(any(Block.class), anyInt(), any(byte[].class))).thenReturn(false);

        kafkaService.processProposal(proposeMessage);

        verify(blockchainService).processProposedBlock(proposedBlock, proposer, signature);
        verifyNoInteractions(kafkaTemplate);
    }

    // Tests accepted proposals are processed correctly
    @Test
    public void testAcceptedProposal() throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        int proposer = 3;
        Block proposedBlock = TestUtils.getRandomBlock();
        String signature = signBase64(proposedBlock, proposer);
        ProposeMessage proposeMessage = new ProposeMessage(proposer, proposedBlock.toJsonBlock(), signature);

        byte[] thisNodeSignature = sign(proposedBlock, nodeId);

        when(blockchainService.processProposedBlock(any(Block.class), anyInt(), any(byte[].class))).thenReturn(true);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(result);
        when(result.get()).thenReturn(null);
        when(result.get(anyLong(), any(TimeUnit.class))).thenReturn(null);
        when(cryptographyService.sign(proposedBlock)).thenReturn(thisNodeSignature);
        when(cryptographyService.signBase64(proposedBlock)).thenReturn(Base64.getEncoder().encodeToString(thisNodeSignature));

        kafkaService.processProposal(proposeMessage);

        verify(blockchainService).processProposedBlock(proposedBlock, proposer, Base64.getDecoder().decode(signature));
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());

        assertEquals("topicName", topicCaptor.getValue());

        JsonNode jsonNode = objectMapper.readTree(messageCaptor.getValue());
        assertEquals(jsonNode.get("sender").intValue(), nodeId);
        assertEquals(jsonNode.get("messageType").textValue(), "vote");

        JsonNode messageNode = jsonNode.get("message");
        assertEquals(messageNode.get("nodeId").intValue(), nodeId);
        assertEquals(messageNode.at("/block/parentHash").textValue(), proposedBlock.getParentHashBase64());
        assertEquals(messageNode.at("/block/epoch").intValue(), proposedBlock.getEpoch());
        assertEquals(messageNode.at("/block/payload").textValue(), proposedBlock.getPayloadBase64());
        assertEquals(messageNode.get("signature").textValue(), Base64.getEncoder().encodeToString(thisNodeSignature));
        assertEquals(messageNode.get("proposerSignature").textValue(), signature);
    }

    // Tests that votes are processed correctly
    @Test
    public void testProcessVote() {
        int voter = 5;
        Block block = TestUtils.getRandomBlock();
        String signature = signBase64(block, voter);
        String proposerSignature = signBase64(block, 1); // random signature - doesn't matter
        VoteMessage voteMessage = new VoteMessage(voter, block.toJsonBlock(), signature, proposerSignature);

        kafkaService.processVote(voteMessage);

        verify(blockchainService).processBlockVote(block, voter, Base64.getDecoder().decode(signature), Base64.getDecoder().decode(proposerSignature));
        verifyNoInteractions(kafkaTemplate);
    }

    // Tests that broadcast functions correctly
    @Test
    public void testBroadcast() throws ExecutionException, InterruptedException, TimeoutException {
        String randomMessage = UUID.randomUUID().toString() + UUID.randomUUID().toString() + UUID.randomUUID().toString() + UUID.randomUUID().toString() + UUID.randomUUID().toString();
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(result);
        when(result.get()).thenReturn(null);
        when(result.get(anyLong(), any(TimeUnit.class))).thenReturn(null);
        kafkaService.broadcast(randomMessage);
        verify(kafkaTemplate).send("topicName", randomMessage);
    }

    private String signBase64(Block block, int signer) {
        return Base64.getEncoder().encodeToString(sign(block, signer));
    }

    // generalized sign from CryptographyService
    private byte[] sign(Block block, int signer) {
        try {
            Signature signature = Signature.getInstance("SHA384withECDSA");
            signature.initSign(getPrivateKey(signer));

            signature.update(block.toBytes());
            return signature.sign();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No SHA384withECDSA algorithm provider.", e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Private key for this node is invalid.", e);
        } catch (SignatureException e) {
            throw new IllegalStateException("Signature object has not been initialized correctly.", e);
        }

    }

    private PrivateKey getPrivateKey(int nodeId) {
        try {
            KeyStore keyStore = getPrivateKeyStore(nodeId);
            return (PrivateKey) keyStore.getKey("node" + nodeId, keystorePassword.toCharArray());
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Keystore has not been initialized correctly.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private KeyStore getPrivateKeyStore(int nodeId) {
        try {
            Path path = Path.of(privateKeyDirectory).resolve("node" + nodeId + "_keystore.p12");
            FileInputStream keystoreFile = new FileInputStream(path.toString());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(keystoreFile, keystorePassword.toCharArray());
            return keyStore;
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Private key for this node not found.", e);
        } catch (KeyStoreException e) {
            throw new IllegalStateException("No PKCS12 keystore provider.", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (CertificateException e) {
            throw new IllegalStateException("Private key certificate for this node not found.", e);
        }
    }

}
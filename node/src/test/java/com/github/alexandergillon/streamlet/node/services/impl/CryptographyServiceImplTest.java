package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CryptographyServiceImplTest {

    @Autowired
    private CryptographyService cryptographyService;

    @Value("${streamlet.testing.keystore.public.location}")
    private String publicKeyLocation;

    @Value("${streamlet.node.id}")
    private int nodeId;

    private static final int EPOCH_WHERE_NODE_1_IS_LEADER = 1;

    // Tests that signing works correctly
    @Test
    public void testSign() throws FileNotFoundException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Block block = TestUtils.getRandomBlock();
        byte[] signatureBytes = cryptographyService.sign(block);

        PublicKey publicKey = getPublicKey(publicKeyLocation);
        Signature signature = Signature.getInstance("SHA384withECDSA");
        signature.initVerify(publicKey);
        signature.update(block.toBytes());
        assertTrue(signature.verify(signatureBytes));
    }

    // Tests that base64 signing works correctly
    @Test
    public void testSignBase64() throws FileNotFoundException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Block block = TestUtils.getRandomBlock();
        String signatureBase64 = cryptographyService.signBase64(block);

        PublicKey publicKey = getPublicKey(publicKeyLocation);
        Signature signature = Signature.getInstance("SHA384withECDSA");
        signature.initVerify(publicKey);
        signature.update(block.toBytes());
        assertTrue(signature.verify(Base64.getDecoder().decode(signatureBase64)));
    }

    // Tests proposal validation - relies on the correctness of CryptographyService.sign()
    @Test
    public void testValidateProposal() {
        Block randomBlock = TestUtils.getRandomBlock();
        Block block = new Block(randomBlock.getParentHash(), EPOCH_WHERE_NODE_1_IS_LEADER, randomBlock.getPayload()); // ensure correct epoch
        byte[] signature = cryptographyService.sign(block);

        assertTrue(cryptographyService.validateProposal(block, signature));
    }

    // Tests that incorrect signatures are rejected - relies on the correctness of CryptographyService.sign()
    @Test
    public void testInvalidProposalSignature() {
        Block randomBlock = TestUtils.getRandomBlock();
        Block block = new Block(randomBlock.getParentHash(), EPOCH_WHERE_NODE_1_IS_LEADER, randomBlock.getPayload()); // ensure correct epoch
        byte[] signature = cryptographyService.sign(block);
        // see following for encoding of ECDSA signatures, and why we change the 6th byte: https://bitcoin.stackexchange.com/questions/12554/why-the-signature-is-always-65-13232-bytes-long/12556#12556
        signature[5]++; // now, signature is certainly incorrect

        assertFalse(cryptographyService.validateProposal(block, signature));
    }

    // Tests vote validation - relies on the correctness of CryptographyService.sign()
    @Test
    public void testValidateVote() {
        Block block = TestUtils.getRandomBlock();
        byte[] signature = cryptographyService.sign(block);

        assertTrue(cryptographyService.validateVote(block, nodeId, signature));
    }

    // Tests that incorrect signatures are rejected - relies on the correctness of CryptographyService.sign()
    @Test
    public void testInvalidVoteSignature() {
        Block block = TestUtils.getRandomBlock();
        byte[] signature = cryptographyService.sign(block);
        // see following for encoding of ECDSA signatures, and why we change the 6th byte: https://bitcoin.stackexchange.com/questions/12554/why-the-signature-is-always-65-13232-bytes-long/12556#12556
        signature[5]++; // now, signature is certainly incorrect

        assertFalse(cryptographyService.validateVote(block, nodeId, signature));
    }

    // Signature is valid, but not for the supplied voter
    @Test
    public void testInvalidVoter() {
        Block block = TestUtils.getRandomBlock();
        byte[] signature = cryptographyService.sign(block);

        assertFalse(cryptographyService.validateVote(block, nodeId+1, signature));
    }

    private PublicKey getPublicKey(String certificatePath) throws CertificateException, FileNotFoundException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        FileInputStream certificateFile = new FileInputStream(certificatePath);
        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certificateFile);
        return certificate.getPublicKey();
    }

}
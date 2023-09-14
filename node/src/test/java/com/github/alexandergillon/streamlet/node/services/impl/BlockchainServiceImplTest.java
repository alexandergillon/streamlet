package com.github.alexandergillon.streamlet.node.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import com.github.alexandergillon.streamlet.node.services.KafkaSendingService;
import com.github.alexandergillon.streamlet.node.services.PayloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
  number under leader is the true leader
  number in brackets is what the blockchainService thinks is the leader due to stubbed behavior of CryptographyService

  epoch     leader
    1         1 (2)
    2         1 (2)
    3         0
    4         4
    5         0
    6         0
    7         0
    8         3
    9         0
    10        1 (2)
    11        2
    12        0
    13        3
    14        1 (2)
    15        0


 */

@SpringBootTest
class BlockchainServiceImplTest {

    @Value("${streamlet.node.id}")
    private int nodeId;
    @Value("${streamlet.participants}")
    private int numNodes;
    @Value("${streamlet.testing.keystore.private.directory}")
    private String privateKeyDirectory;
    @Value("${streamlet.testing.keystore.private.password}")
    private String keystorePassword;

    @SpyBean
    private CryptographyService cryptographyService;

    @MockBean
    private PayloadService payloadService;

    @MockBean
    private KafkaSendingService kafkaSendingService;

    @Autowired
    private BlockchainService blockchainService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setupSpy() {
        Answer<Integer> answer = invocationOnMock -> {
            int realLeader = (int) invocationOnMock.callRealMethod();
            if (realLeader == nodeId) {
                return (nodeId + 1) % numNodes;
            } else {
                return realLeader;
            }
        };

        when(cryptographyService.leaderForEpoch(anyInt())).thenAnswer(answer);
    }

    /* epoch
     * 1                    1
     *                      |
     * 2                    2
     *                      |
     * 3                    3     |               ???
     *                      |     |                |
     * 4                    4     |   epoch 100:   25 (not connected to the rest)
     *                      |
     * 5                    5
     *                      |
     * 6                    6
     *                      |
     * 7                    7
     *              /----/--|--\----\---------------\------\-----\
     * 8           8    9   10  11   12             26     |     |
     *             |    |   |   |     \-----\        |     |     |
     * 9          13   14   |  16     17    20       |     27    |
     *             |    |   |   |       \            |     |     |
     * 10         18   19   15  21      22           28    |     |
     *                                /----\         |     |     |
     * 11                            23    24        |     29    |
     *                                               |     |     |
     * 12                                            |     33    34
     *                                               |
     * 13                                            30
     *                                               |
     * 14                                            31
     *                                               |
     * 15                                            32
     *
     */
    private List<Block> blocks;

    // Sets up the test blocks. See diagram above.
    @BeforeEach
    public void setupBlocks() {
        Block block1 = new Block(Block.GENESIS_BLOCK.getHash(), 1, TestUtils.randomPayload());
        Block block2 = new Block(block1.getHash(), 2, TestUtils.randomPayload());
        Block block3 = new Block(block2.getHash(), 3, TestUtils.randomPayload());
        Block block4 = new Block(block3.getHash(), 4, TestUtils.randomPayload());
        Block block5 = new Block(block4.getHash(), 5, TestUtils.randomPayload());
        Block block6 = new Block(block5.getHash(), 6, TestUtils.randomPayload());
        Block block7 = new Block(block6.getHash(), 7, TestUtils.randomPayload());
        Block block8 = new Block(block7.getHash(), 8, TestUtils.randomPayload());
        Block block9 = new Block(block7.getHash(), 8, TestUtils.randomPayload());
        Block block10 = new Block(block7.getHash(), 8, TestUtils.randomPayload());
        Block block11 = new Block(block7.getHash(), 8, TestUtils.randomPayload());
        Block block12 = new Block(block7.getHash(), 8, TestUtils.randomPayload());
        Block block13 = new Block(block8.getHash(), 9, TestUtils.randomPayload());
        Block block14 = new Block(block9.getHash(), 9, TestUtils.randomPayload());
        Block block15 = new Block(block10.getHash(), 10, TestUtils.randomPayload());
        Block block16 = new Block(block11.getHash(), 9, TestUtils.randomPayload());
        Block block17 = new Block(block12.getHash(), 9, TestUtils.randomPayload());
        Block block18 = new Block(block13.getHash(), 10, TestUtils.randomPayload());
        Block block19 = new Block(block14.getHash(), 10, TestUtils.randomPayload());
        Block block20 = new Block(block12.getHash(), 9, TestUtils.randomPayload());
        Block block21 = new Block(block16.getHash(), 10, TestUtils.randomPayload());
        Block block22 = new Block(block17.getHash(), 10, TestUtils.randomPayload());
        Block block23 = new Block(block22.getHash(), 11, TestUtils.randomPayload());
        Block block24 = new Block(block22.getHash(), 11, TestUtils.randomPayload());
        Block block26 = new Block(block7.getHash(), 8, TestUtils.randomPayload());
        Block block27 = new Block(block7.getHash(), 9, TestUtils.randomPayload());
        Block block28 = new Block(block26.getHash(), 10, TestUtils.randomPayload());
        Block block29 = new Block(block27.getHash(), 11, TestUtils.randomPayload());
        Block block30 = new Block(block28.getHash(), 13, TestUtils.randomPayload());
        Block block31 = new Block(block30.getHash(), 14, TestUtils.randomPayload());
        Block block32 = new Block(block31.getHash(), 15, TestUtils.randomPayload());
        Block block33 = new Block(block29.getHash(), 12, TestUtils.randomPayload());
        Block block34 = new Block(block7.getHash(), 12, TestUtils.randomPayload());

        blocks = new ArrayList<>(Arrays.asList(Block.GENESIS_BLOCK, block1, block2, block3, block4, block5, block6, block7, block8,
                block9, block10, block11, block12, block13, block14, block15, block16, block17, block18, block19,
                block20, block21, block22, block23, block24,
                Block.GENESIS_BLOCK, // for padding, will be overwritten later
                block26, block27, block28, block29, block30, block31, block32, block33, block34));

        byte[] hashNotInBlocks = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(hashNotInBlocks);
        outer: while (true) {
            for (Block block : blocks) {
                if (Arrays.equals(hashNotInBlocks, block.getHash())) {
                    ThreadLocalRandom.current().nextBytes(hashNotInBlocks);
                    continue outer;
                }
            }
            break;
        }

        Block block25 = new Block(hashNotInBlocks, 100, TestUtils.randomPayload());
        blocks.set(25, block25);
    }

    // Tests that stubbed behavior of spied CryptographyService works correctly
    @Test
    public void testSpiedCryptographyService() {
        // tests that our spy works
        for (int i = 0; i < 100000; i++) {
            int leader = cryptographyService.leaderForEpoch(i);
            assertNotEquals(nodeId, leader);
        }

        // tests that our spy is injected correctly
        CryptographyService blockchainServiceCryptographyService = (CryptographyService) ReflectionTestUtils.getField(blockchainService, "cryptographyService");
        assert blockchainServiceCryptographyService != null;
        for (int i = 0; i < 100000; i++) {
            int leader = blockchainServiceCryptographyService.leaderForEpoch(i);
            assertNotEquals(nodeId, leader);
        }
    }

    // Tests that BlockchainService.setEpoch() throws correct exceptions
    @Test
    @DirtiesContext
    public void testSetEpochExceptionEqual() {
        assertDoesNotThrow(() -> blockchainService.setEpoch(0));
        assertDoesNotThrow(() -> blockchainService.setEpoch(1));
        assertDoesNotThrow(() -> blockchainService.setEpoch(2));
        assertDoesNotThrow(() -> blockchainService.setEpoch(3));
        assertDoesNotThrow(() -> blockchainService.setEpoch(5));
        assertThrows(IllegalArgumentException.class, () -> blockchainService.setEpoch(5));
    }

    // Tests that BlockchainService.setEpoch() throws correct exceptions
    @Test
    @DirtiesContext
    public void testSetEpochExceptionLess() {
        assertDoesNotThrow(() -> blockchainService.setEpoch(0));
        assertDoesNotThrow(() -> blockchainService.setEpoch(1));
        assertDoesNotThrow(() -> blockchainService.setEpoch(2));
        assertDoesNotThrow(() -> blockchainService.setEpoch(3));
        assertDoesNotThrow(() -> blockchainService.setEpoch(5));
        assertThrows(IllegalArgumentException.class, () -> blockchainService.setEpoch(4));
    }

    // Tests that negative epoch throws exception
    @Test
    @DirtiesContext
    public void testNegativeEpoch() {
        assertThrows(IllegalArgumentException.class, () -> blockchainService.setEpoch(-100));
    }

    // Tests ideal behavior (message delivery is in-order, on time, and no forking of the chain).
    @Test
    @DirtiesContext
    public void testIdealBehavior() {
        String test =
            """
            e1:
            n2 propose b1
            n3 vote b1
            n4 vote b1
            
            e2:
            n2 propose b2
            n0 vote b2
            assert chain b0
            n4 vote b2
            assert chain b0 b1
            
            e3:
            n0 propose b3
            n2 vote b3
            assert chain b0 b1
            n4 vote b3
            assert chain b0 b1 b2
            
            e4:
            n4 propose b4
            n2 vote b4
            assert chain b0 b1 b2
            n3 vote b4
            assert chain b0 b1 b2 b3
            n0 vote b4
            
            e5:
            n0 propose b5
            n3 vote b5
            assert chain b0 b1 b2 b3
            n4 vote b5
            assert chain b0 b1 b2 b3 b4
            
            e6:
            n0 propose b6
            n2 vote b6
            assert chain b0 b1 b2 b3 b4
            n4 vote b6
            assert chain b0 b1 b2 b3 b4 b5
            
            e7:
            n0 propose b7
            n2 vote b7
            assert chain b0 b1 b2 b3 b4 b5
            n3 vote b7
            assert chain b0 b1 b2 b3 b4 b5 b6
            """;
        doTest(test);
    }

    // Tests that blocks are not finalized if the three notarized blocks in a row are not of consecutive epochs
    @Test
    @DirtiesContext
    public void testMissedEpoch() {
        String test =
                """
                e1:
                n2 propose b1
                n3 vote b1
                n4 vote b1
                
                e2:
                n2 propose b2
                n3 vote b2
                n4 vote b2
                
                e3:
                n0 propose b3
                n2 vote b3
                n3 vote b3
                
                e4:
                n4 propose b4
                n2 vote b4
                n3 vote b4
                
                e5:
                n0 propose b5
                n2 vote b5
                n3 vote b5
                
                e6:
                n0 propose b6
                n2 vote b6
                n3 vote b6
                
                e7:
                n0 propose b7
                n2 vote b7
                n3 vote b7
                
                e8:
                n3 propose b10
                n2 vote b10
                n4 vote b10
                
                e10:
                n2 propose b15
                n3 vote b15
                n4 vote b15
                
                assert chain b0 b1 b2 b3 b4 b5 b6 b7
                """;
        doTest(test);
    }

    // Tests a scenario in which this node is delayed from the rest of the network. I.e. the rest of the network
    // is still making progress, but this node is hearing about it late
    @Test
    @DirtiesContext
    public void testNetworkDelay() {
        // novote are because this node does not vote on the proposal due to hearing about it late
        String test =
                """
                e2:
                n2 propose b1 novote
                n3 vote b1
                n4 vote b1
                n0 vote b1
                
                e3:
                n2 propose b2 novote
                n0 vote b2
                n3 vote b2
                n4 vote b2
                
                e4:
                n0 propose b3 novote
                n2 vote b3
                n3 vote b3
                n4 vote b3
                
                e5:
                n4 propose b4 novote
                n0 vote b4
                n3 vote b4
                n4 vote b4
                
                e6:
                n0 propose b5 novote
                n2 vote b5
                n3 vote b5
                n4 vote b5
                
                e7:
                n0 propose b6 novote
                n2 vote b6
                n3 vote b6
                n4 vote b6
                
                e8:
                n0 propose b7 novote
                n2 vote b7
                n3 vote b7
                n4 vote b7
                
                assert chain b0 b1 b2 b3 b4 b5 b6
                """;
        doTest(test);
    }

    // Tests that a non-leader proposing blocks does not interfere with normal operation
    @Test
    @DirtiesContext
    public void testBadProposer() {
        String test = """
            e1:
            n2 propose b1
            n3 vote b1
            n4 vote b1
            
            e2:
            n2 propose b2
            n0 vote b2
            n4 vote b2
            
            e3:
            n0 propose b3
            n2 vote b3
            n4 vote b3
            
            e4:
            n4 propose b4
            n2 vote b4
            n3 vote b4
            n0 vote b4
            
            e5:
            n0 propose b5
            n3 vote b5
            n4 vote b5
            
            e6:
            n0 propose b6
            n2 vote b6
            n4 vote b6
            
            e7:
            n0 propose b7
            n2 vote b7
            n3 vote b7
            
            e8:
            n2 propose b8 novote
            n3 propose b9
            n0 vote b9
            n4 vote b9
            
            e9:
            n2 propose b13 novote
            n0 propose b14
            n3 vote b14
            n4 vote b14
            
            assert chain b0 b1 b2 b3 b4 b5 b6 b7 b9
            """;
        doTest(test);
    }

    // Tests byzantine behavior where a node impersonates another node's proposals, but their signatures are incorrect
    @Test
    @DirtiesContext
    public void testBadProposalSignature() {
        String test = """
            e1:
            n2 propose b1
            n3 vote b1
            n4 vote b1
            
            e2:
            n2 propose b2
            n0 vote b2
            n4 vote b2
            
            e3:
            n0 propose b3
            n2 vote b3
            n4 vote b3
            
            e4:
            n4 propose b4
            n2 vote b4
            n3 vote b4
            n0 vote b4
            
            e5:
            n0 propose b5
            n3 vote b5
            n4 vote b5
            
            e6:
            n0 propose b6
            n2 vote b6
            n4 vote b6
            
            e7:
            n0 propose b7
            n2 vote b7
            n3 vote b7
            
            e8:
            n3 propose b8 badsig novote
            n3 propose b9
            n0 vote b9
            n4 vote b9
            
            e9:
            n0 propose b13 badsig novote
            n0 propose b14
            n3 vote b14
            n4 vote b14
            
            assert chain b0 b1 b2 b3 b4 b5 b6 b7 b9
            """;
        doTest(test);
    }

    // Tests byzantine behavior where a node impersonates another node's votes, but their signatures are incorrect
    @Test
    @DirtiesContext
    public void testBadVoteSignature() {
        // novotes are because the proposals don't extend the longest notarized chain
        String test = """
            e1:
            n2 propose b1
            n3 vote b1
            n4 vote b1
            
            e2:
            n2 propose b2
            n0 vote b2
            n4 vote b2
            
            e3:
            n0 propose b3
            n2 vote b3
            n4 vote b3
            
            e4:
            n4 propose b4
            n2 vote b4
            n3 vote b4
            n0 vote b4
            
            e5:
            n0 propose b5
            n3 vote b5 badsig
            n4 vote b5 badsig
            
            e6:
            n0 propose b6 novote
            n2 vote b6 badsig
            n4 vote b6 badsig
            
            e7:
            n0 propose b7 novote
            n2 vote b7 badsig
            n3 vote b7 badsig
            
            assert chain b0 b1 b2 b3
            """;
        doTest(test);
    }

    // Tests byzantine behavior where a node impersonates another node's proposals via a bad vote, but their signatures are incorrect
    @Test
    @DirtiesContext
    public void testBadVoteProposerSignature() {
        String test = """
            e1:
            n2 propose b1
            n3 vote b1
            n4 vote b1
            
            e2:
            n2 propose b2
            n0 vote b2
            n4 vote b2
            
            e3:
            n0 propose b3
            n2 vote b3
            n4 vote b3
            
            e4:
            n4 propose b4
            n2 vote b4
            n3 vote b4
            n0 vote b4
            
            e5:
            n0 propose b5
            n3 vote b5
            n4 vote b5
            
            e6:
            n0 propose b6
            n2 vote b6
            n4 vote b6
            
            e7:
            n0 propose b7
            n2 vote b7
            n3 vote b7
            
            e8:
            n0 vote b8 badproposersig
            n3 propose b26
            n0 vote b26
            n4 vote b26
            
            e9:
            n0 propose b13 novote
            
            e10:
            n2 propose b28
            n3 vote b28
            n4 vote b28
            
            e13:
            n3 propose b30
            n2 vote b30
            n4 vote b30
            
            e14:
            n2 propose b31
            n3 vote b31
            n4 vote b31
            
            e15:
            n0 propose b32
            n2 vote b32
            n3 vote b32
            
            assert chain b0 b1 b2 b3 b4 b5 b6 b7 b26 b28 b30 b31
            """;
        doTest(test);
    }

    // Tests that the blockchain proposes blocks correctly
    @Test
    @DirtiesContext
    public void testSimpleProposal() throws JsonProcessingException {
        String setup = """
            e1:
            n2 propose b1
            n3 vote b1
            n4 vote b1
            
            e2:
            n2 propose b2
            n0 vote b2
            n4 vote b2
            
            e3:
            n0 propose b3
            n2 vote b3
            n4 vote b3
            
            e4:
            n4 propose b4
            n2 vote b4
            n3 vote b4
            n0 vote b4
            
            e5:
            """;
        doTest(setup);
        int thisEpoch = 5;
        byte[] payload = TestUtils.randomPayload();
        byte[] signature = TestUtils.randomPayload();

        //noinspection unchecked
        when(payloadService.getNextPayload(any(Set.class))).thenReturn(payload);
        doReturn(signature).when(cryptographyService).sign(any(Block.class));
        doReturn(Base64.getEncoder().encodeToString(signature)).when(cryptographyService).signBase64(any(Block.class));

        blockchainService.proposeBlock();

        ArgumentCaptor<String> broadcastString = ArgumentCaptor.forClass(String.class);
        verify(kafkaSendingService).broadcast(broadcastString.capture());

        JsonNode jsonNode = objectMapper.readTree(broadcastString.getValue());
        assertEquals(jsonNode.get("sender").intValue(), nodeId);
        assertEquals(jsonNode.get("messageType").textValue(), "propose");

        JsonNode messageNode = jsonNode.get("message");
        assertEquals(messageNode.get("nodeId").intValue(), nodeId);
        assertEquals(messageNode.at("/block/parentHash").textValue(), blocks.get(4).getHashBase64());
        assertEquals(messageNode.at("/block/epoch").intValue(), thisEpoch);
        assertEquals(messageNode.at("/block/payload").textValue(), Base64.getEncoder().encodeToString(payload));
        assertEquals(messageNode.get("signature").textValue(), Base64.getEncoder().encodeToString(signature));
    }

    // Tests that the blockchain proposes blocks correctly when unnotarized blocks exist
    @Test
    @DirtiesContext
    public void testProposalWithUnfinalizedBlocks() throws JsonProcessingException {
        String setup = """
            e1:
            n2 propose b1
            n3 vote b1
            n4 vote b1
            
            e2:
            n2 propose b2
            n0 vote b2
            n4 vote b2
            
            e3:
            n0 propose b3
            n2 vote b3
            n4 vote b3
            
            e4:
            n4 propose b4
            n3 vote b4
            
            e5:
            """;
        doTest(setup);
        int thisEpoch = 5;
        byte[] payload = TestUtils.randomPayload();
        byte[] signature = TestUtils.randomPayload();

        //noinspection unchecked
        when(payloadService.getNextPayload(any(Set.class))).thenReturn(payload);
        doReturn(signature).when(cryptographyService).sign(any(Block.class));
        doReturn(Base64.getEncoder().encodeToString(signature)).when(cryptographyService).signBase64(any(Block.class));

        blockchainService.proposeBlock();

        ArgumentCaptor<String> broadcastString = ArgumentCaptor.forClass(String.class);
        verify(kafkaSendingService).broadcast(broadcastString.capture());

        JsonNode jsonNode = objectMapper.readTree(broadcastString.getValue());
        assertEquals(jsonNode.get("sender").intValue(), nodeId);
        assertEquals(jsonNode.get("messageType").textValue(), "propose");

        JsonNode messageNode = jsonNode.get("message");
        assertEquals(messageNode.get("nodeId").intValue(), nodeId);
        assertEquals(messageNode.at("/block/parentHash").textValue(), blocks.get(3).getHashBase64());
        assertEquals(messageNode.at("/block/epoch").intValue(), thisEpoch);
        assertEquals(messageNode.at("/block/payload").textValue(), Base64.getEncoder().encodeToString(payload));
        assertEquals(messageNode.get("signature").textValue(), Base64.getEncoder().encodeToString(signature));
    }

    private void doTest(String test) {
        List<String> commands = test.lines().toList();
        for (String command : commands) {
            if (command.isEmpty()) continue; // allow blank lines, for readability
            System.out.println("processing command: " + command);

            String[] tokens = command.split(" ");
            List<String> tokenList = Arrays.asList(tokens);
            String firstToken = tokens[0];

            // epoch change command
            if (firstToken.charAt(0) == 'e') {
                blockchainService.setEpoch(Integer.parseInt(firstToken.substring(1, firstToken.length()-1)));
            }

            // node action command
            else if (firstToken.charAt(0) == 'n') {
                int nodeId = Integer.parseInt(firstToken.substring(1));
                String action = tokens[1];
                int blockIndex = Integer.parseInt(tokens[2].substring(1));
                Block block = blocks.get(blockIndex);

                if (action.equals("propose")) {
                    boolean accepted = true;
                    byte[] signature = sign(block, nodeId);
                    if (tokenList.contains("badsig")) {
                        // see following for encoding of ECDSA signatures, and why we change the 6th byte: https://bitcoin.stackexchange.com/questions/12554/why-the-signature-is-always-65-13232-bytes-long/12556#12556
                        signature[5]++; // now, signature is certainly incorrect
                    }
                    if (tokenList.contains("novote")) accepted = false;
                    assertEquals(accepted, blockchainService.processProposedBlock(block, nodeId, signature));
                } else if (action.equals("vote")) {
                    byte[] signature = sign(block, nodeId);
                    if (tokenList.contains("badsig")) {
                        // see following for encoding of ECDSA signatures, and why we change the 6th byte: https://bitcoin.stackexchange.com/questions/12554/why-the-signature-is-always-65-13232-bytes-long/12556#12556
                        signature[5]++; // now, signature is certainly incorrect
                    }
                    blockchainService.processBlockVote(block, nodeId, signature, sign(block, cryptographyService.leaderForEpoch(block.getEpoch())));
                } else {
                    throw new IllegalArgumentException("Cannot parse command: " + command);
                }
            }

            // assertion command
            else if (firstToken.equals("assert")) {
                String secondToken = tokens[1];

                // switch expressions (vs statements) do not fall through
                switch (secondToken) {
                    case "chain" -> {
                        if (tokens.length < 3) throw new IllegalArgumentException("Cannot parse command: " + command);
                        ArrayList<Block> expectedChain = new ArrayList<>();
                        for (int i = 2; i < tokens.length; i++) {
                            int blockIndex = Integer.parseInt(tokens[i].substring(1));
                            expectedChain.add(blocks.get(blockIndex));
                        }
                        assertEquals(expectedChain, blockchainService.getFinalizedChain());
                    }
                    default -> throw new IllegalArgumentException("Cannot parse command: " + command);
                }
            }

            // unrecognized command
            else {
                throw new IllegalArgumentException("Cannot parse command: " + command);
            }
        }
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
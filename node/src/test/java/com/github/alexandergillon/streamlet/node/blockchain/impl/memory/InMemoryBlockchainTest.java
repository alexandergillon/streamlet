package com.github.alexandergillon.streamlet.node.blockchain.impl.memory;

import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.blockchain.Blockchain;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.InvalidBlockException;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.UnknownBlockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBlockchainTest {

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

    // Trace for the first 7 blocks proceeding ideally. With a notarization threshold of 4, all but the last block
    // should be finalized, and all should be notarized
    private static final String first7BlocksIdealNetworkNotarizationThreshold4 =
        """
        e1:
        n1 propose b1
        n2 vote b1
        n3 vote b1
        
        e2:
        n1 propose b2
        n2 vote b2
        n3 vote b2
        
        e3:
        n1 propose b3
        n2 vote b3
        n3 vote b3
        
        e4:
        n1 propose b4
        n2 vote b4
        n3 vote b4
        
        e5:
        n1 propose b5
        n2 vote b5
        n3 vote b5
        
        e6:
        n1 propose b6
        n2 vote b6
        n3 vote b6
        
        e7:
        n1 propose b7
        n2 vote b7
        n3 vote b7
        
        """;

    // Sets up the test blocks. See diagram above.
    @BeforeEach
    public void setupBlocks() {
        Block block1 = new Block(Block.GENESIS_BLOCK.getHash(), 1, randomPayload());
        Block block2 = new Block(block1.getHash(), 2, randomPayload());
        Block block3 = new Block(block2.getHash(), 3, randomPayload());
        Block block4 = new Block(block3.getHash(), 4, randomPayload());
        Block block5 = new Block(block4.getHash(), 5, randomPayload());
        Block block6 = new Block(block5.getHash(), 6, randomPayload());
        Block block7 = new Block(block6.getHash(), 7, randomPayload());
        Block block8 = new Block(block7.getHash(), 8, randomPayload());
        Block block9 = new Block(block7.getHash(), 8, randomPayload());
        Block block10 = new Block(block7.getHash(), 8, randomPayload());
        Block block11 = new Block(block7.getHash(), 8, randomPayload());
        Block block12 = new Block(block7.getHash(), 8, randomPayload());
        Block block13 = new Block(block8.getHash(), 9, randomPayload());
        Block block14 = new Block(block9.getHash(), 9, randomPayload());
        Block block15 = new Block(block10.getHash(), 10, randomPayload());
        Block block16 = new Block(block11.getHash(), 9, randomPayload());
        Block block17 = new Block(block12.getHash(), 9, randomPayload());
        Block block18 = new Block(block13.getHash(), 10, randomPayload());
        Block block19 = new Block(block14.getHash(), 10, randomPayload());
        Block block20 = new Block(block12.getHash(), 9, randomPayload());
        Block block21 = new Block(block16.getHash(), 10, randomPayload());
        Block block22 = new Block(block17.getHash(), 10, randomPayload());
        Block block23 = new Block(block22.getHash(), 11, randomPayload());
        Block block24 = new Block(block22.getHash(), 11, randomPayload());
        Block block26 = new Block(block7.getHash(), 8, randomPayload());
        Block block27 = new Block(block7.getHash(), 9, randomPayload());
        Block block28 = new Block(block26.getHash(), 10, randomPayload());
        Block block29 = new Block(block27.getHash(), 11, randomPayload());
        Block block30 = new Block(block28.getHash(), 13, randomPayload());
        Block block31 = new Block(block30.getHash(), 14, randomPayload());
        Block block32 = new Block(block31.getHash(), 15, randomPayload());
        Block block33 = new Block(block29.getHash(), 12, randomPayload());
        Block block34 = new Block(block7.getHash(), 12, randomPayload());

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

        Block block25 = new Block(hashNotInBlocks, 100, randomPayload());
        blocks.set(25, block25);
    }

    // Tests ideal behavior (message delivery is in-order, on time, and no forking of the chain).
    @Test
    public void testIdealBehavior() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        String test =
            """
            e1:
            n1 propose b1
            assert contains b1
            assert !notarized b1
            n2 vote b1
            assert !notarized b1
            n3 vote b1
            assert notarized b1
            
            e2:
            n1 propose b2
            assert contains b2
            assert !notarized b2
            n2 vote b2
            assert !notarized b2
            assert !finalized b1
            n3 vote b2
            assert notarized b2
            assert finalized b1
            
            e3:
            n1 propose b3
            assert contains b3
            assert !notarized b3
            n2 vote b3
            assert !notarized b3
            assert !finalized b2
            n3 vote b3
            assert notarized b3
            assert finalized b2
            
            e4:
            n1 propose b4
            assert contains b4
            assert !notarized b4
            n2 vote b4
            assert !notarized b4
            assert !finalized b3
            n3 vote b4
            assert notarized b4
            assert finalized b3
            
            e5:
            n1 propose b5
            assert contains b5
            assert !notarized b5
            n2 vote b5
            assert !notarized b5
            assert !finalized b4
            n3 vote b5
            assert notarized b5
            assert finalized b4
            
            e6:
            n1 propose b6
            assert contains b6
            assert !notarized b6
            n2 vote b6
            assert !notarized b6
            assert !finalized b5
            n3 vote b6
            assert notarized b6
            assert finalized b5
            
            e7:
            n1 propose b7
            assert contains b7
            assert !notarized b7
            n2 vote b7
            assert !notarized b7
            assert !finalized b6
            n3 vote b7
            assert notarized b7
            assert finalized b6
            
            assert parent b1 b0
            assert parent b2 b1
            assert parent b3 b2
            assert parent b4 b3
            assert parent b5 b4
            assert parent b6 b5
            assert parent b7 b6
            """;

        doTest(test, blockchain);

        List<Block> expectedFinalizedChain = List.of(blocks.get(0), blocks.get(1), blocks.get(2), blocks.get(3),
                blocks.get(4), blocks.get(5), blocks.get(6));
        assertEquals(expectedFinalizedChain, blockchain.getFinalizedChain());
    }

    // Tests that the return value of Blockchain.processProposedBlock() is correct
    @Test
    public void testReturnValues() throws InvalidBlockException, UnknownBlockException {
        // valid proposal, should vote
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        assertTrue(blockchain.processProposedBlock(blocks.get(1), 1, 1, true));

        // not first proposal of epoch, shouldn't vote
        blockchain = new InMemoryBlockchain(0, 4);
        assertFalse(blockchain.processProposedBlock(blocks.get(1), 1, 1, false));

        // proposal is from previous epoch, shouldn't vote
        blockchain = new InMemoryBlockchain(0, 4);
        assertFalse(blockchain.processProposedBlock(blocks.get(1), 1, 2, true));

        // proposal does not extend longest notarized chain
        blockchain = new InMemoryBlockchain(0, 4);
        doTest(first7BlocksIdealNetworkNotarizationThreshold4, blockchain);
        // This setup gets blocks 11 and 16 notarized
        String moreSetup =
            """
            e8:
            n1 propose b11
            n2 vote b11
            n3 vote b11
            assert notarized b11
            
            e9:
            n1 propose b16
            n2 vote b16
            n3 vote b16
            assert notarized b16
            """;
        doTest(moreSetup, blockchain);
        assertFalse(blockchain.processProposedBlock(blocks.get(34), 4, 12, true));
    }

    // Tests that blocks are not finalized if the three notarized blocks in a row are not of consecutive epochs
    @Test
    public void testMissedEpoch() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        String test =
            """
            e1:
            n1 propose b1
            n2 vote b1
            n3 vote b1
            
            e2:
            n1 propose b2
            n2 vote b2
            n3 vote b2
            
            e3:
            n1 propose b3
            n2 vote b3
            n3 vote b3
            
            e4:
            n1 propose b4
            n2 vote b4
            n3 vote b4
            
            e5:
            n1 propose b5
            n2 vote b5
            n3 vote b5
            
            e6:
            n1 propose b6
            n2 vote b6
            n3 vote b6
            
            e7:
            n1 propose b7
            n2 vote b7
            n3 vote b7
            
            e8:
            n1 propose b10
            n2 vote b10
            n3 vote b10
            
            e10:
            n1 propose b15
            n2 vote b15
            n3 vote b15
            """;
        doTest(test, blockchain);
        List<Block> expectedFinalizedChain = List.of(blocks.get(0), blocks.get(1), blocks.get(2), blocks.get(3),
                blocks.get(4), blocks.get(5), blocks.get(6), blocks.get(7));
        assertEquals(expectedFinalizedChain, blockchain.getFinalizedChain());
        assertTrue(blockchain.isNotarized(blocks.get(10)));
        assertTrue(blockchain.isNotarized(blocks.get(15)));
    }

    // Tests a scenario in which this node is delayed from the rest of the network. I.e. the rest of the network
    // is still making progress, but this node is hearing about it late
    @Test
    public void testNetworkDelay() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        String test =
            """
            e2:
            n1 propose b1
            n2 vote b1
            n3 vote b1
            assert !notarized b1
            n4 vote b1
            assert notarized b1
            
            e3:
            n1 propose b2
            n2 vote b2
            n3 vote b2
            assert !notarized b2
            n4 vote b2
            assert notarized b2
            
            e4:
            n1 propose b3
            n2 vote b3
            n3 vote b3
            assert !notarized b3
            assert !finalized b2
            n4 vote b3
            assert notarized b3
            assert finalized b2
            
            e5:
            n1 propose b4
            n2 vote b4
            n3 vote b4
            assert !notarized b4
            n4 vote b4
            assert notarized b4
            
            e6:
            n1 propose b5
            n2 vote b5
            n3 vote b5
            assert !notarized b5
            n4 vote b5
            assert notarized b5
            
            e7:
            n1 propose b6
            n2 vote b6
            n3 vote b6
            assert !notarized b6
            assert !finalized b5
            n4 vote b6
            assert notarized b6
            assert finalized b5
            
            e8:
            n1 propose b7
            n2 vote b7
            n3 vote b7
            assert !notarized b7
            n4 vote b7
            assert notarized b7
            """;
        doTest(test, blockchain);
        List<Block> expectedFinalizedChain = List.of(blocks.get(0), blocks.get(1), blocks.get(2), blocks.get(3),
                blocks.get(4), blocks.get(5), blocks.get(6));
        assertEquals(expectedFinalizedChain, blockchain.getFinalizedChain());
    }

    // Test a scenario in which a fork occurs, due to network instability. Only one branch should be finalized
    @Test
    public void testFork() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        // Voting patterns as seen below are possible if (for whatever reason) vote messages
        String test = first7BlocksIdealNetworkNotarizationThreshold4 +
                """
                e8:
                n1 propose b26
                
                e9:
                n2 propose b27
                
                n2 vote b26
                n3 vote b26
                n1 vote b27
                n3 vote b27
                
                assert notarized b26
                assert notarized b27
                
                e10:
                n1 propose b28
                
                e11:
                n2 propose b29
                
                n2 vote b28
                n3 vote b28
                n1 vote b29
                n3 vote b29
                
                assert notarized b28
                assert notarized b29
                
                e12:
                n2 propose b33
                
                e13:
                n1 propose b30
                
                n1 vote b33
                n3 vote b33
                n2 vote b30
                n3 vote b30
                
                assert notarized b33
                assert notarized b30
                
                e14:
                n1 propose b31
                n2 vote b31
                n3 vote b31
                assert notarized b31
                
                e15:
                n1 propose b32
                n2 vote b32
                n3 vote b32
                assert notarized b32
                
                assert finalized b7
                assert finalized b26
                assert finalized b28
                assert finalized b30
                assert finalized b31
                assert !finalized b32
                
                assert !finalized b27
                assert !finalized b29
                assert !finalized b33
                """;
        doTest(test, blockchain);
        List<Block> expectedFinalizedChain = List.of(blocks.get(0), blocks.get(1), blocks.get(2), blocks.get(3),
                blocks.get(4), blocks.get(5), blocks.get(6), blocks.get(7), blocks.get(26), blocks.get(28),
                blocks.get(30), blocks.get(31));
        assertEquals(expectedFinalizedChain, blockchain.getFinalizedChain());
    }

    // Tests that the blockchain is not reliant on a specific notarization order
    @Test
    public void testNotarizationOrder() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        String test =
            """
            e1:
            n1 propose b1
            
            e2:
            n1 propose b2
            
            e3:
            n1 propose b3
            
            n2 vote b1
            n3 vote b1
            n4 vote b1
            
            n2 vote b3
            n3 vote b3
            n4 vote b3
            
            assert !finalized b1
            assert !finalized b2
            
            n2 vote b2
            n3 vote b2
            n4 vote b2
            
            assert finalized b1
            assert finalized b2
            """;
        doTest(test, blockchain);
    }

    // Tests that the blockchain is not reliant on a specific notarization order
    @Test
    public void testNotarizationOrder2() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        String test =
            """
            e1:
            n1 propose b1
            
            e2:
            n1 propose b2
            
            e3:
            n1 propose b3
            
            n2 vote b2
            n3 vote b2
            n4 vote b2
            
            n2 vote b3
            n3 vote b3
            n4 vote b3
            
            assert !finalized b1
            assert !finalized b2
            
            n2 vote b1
            n3 vote b1
            n4 vote b1
            
            assert finalized b1
            assert finalized b2
            """;
        doTest(test, blockchain);
    }

    // Tests that the blockchain is not reliant on a specific notarization order
    @Test
    public void testNotarizationOrder3() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        String test =
            """
            e1:
            n1 propose b1
            
            e2:
            n1 propose b2
            
            e3:
            n1 propose b3
            
            e4:
            n1 propose b4
            
            e5:
            n1 propose b5
            
            e6:
            n1 propose b6
            
            e7:
            n1 propose b7
            
            n2 vote b1
            n3 vote b1
            n4 vote b1
            
            n2 vote b3
            n3 vote b3
            n4 vote b3
            
            n2 vote b5
            n3 vote b5
            n4 vote b5
            
            n2 vote b7
            n3 vote b7
            n4 vote b7
            
            assert !finalized b1
            assert !finalized b2
            assert !finalized b3
            assert !finalized b4
            assert !finalized b5
            assert !finalized b6
            
            n2 vote b2
            n3 vote b2
            n4 vote b2
            
            assert finalized b1
            assert finalized b2
            assert !finalized b3
            assert !finalized b4
            assert !finalized b5
            assert !finalized b6
            
            n2 vote b4
            n3 vote b4
            n4 vote b4
            
            assert finalized b1
            assert finalized b2
            assert finalized b3
            assert finalized b4
            assert !finalized b5
            assert !finalized b6
            
            n2 vote b6
            n3 vote b6
            n4 vote b6
            
            assert finalized b1
            assert finalized b2
            assert finalized b3
            assert finalized b4
            assert finalized b5
            assert finalized b6
            """;
        doTest(test, blockchain);
    }

    // Tests that a block that was not notarized still becomes finalized if later in the chain becomes finalized
    @Test
    public void testSkippedBlockFinalization() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        String test =
                """
                e1:
                n1 propose b1
                
                e2:
                n1 propose b2
                
                e3:
                n1 propose b3
                
                e4:
                n1 propose b4
                
                e5:
                n1 propose b5
                
                n2 vote b1
                n3 vote b1
                n4 vote b1
                
                n2 vote b3
                n3 vote b3
                n4 vote b3
                
                n2 vote b4
                n3 vote b4
                n4 vote b4
                
                assert !finalized b1
                assert !finalized b2
                assert !finalized b3
                assert !finalized b4
                
                n2 vote b5
                n3 vote b5
                n4 vote b5
                
                assert finalized b1
                assert finalized b2
                assert finalized b3
                assert finalized b4
                """;
        doTest(test, blockchain);
    }

    // Tests that correct exceptions are thrown when a bad propose occurs
    @Test
    public void testBadPropose() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        doTest(first7BlocksIdealNetworkNotarizationThreshold4, blockchain);
        assertThrows(UnknownBlockException.class, () -> blockchain.processProposedBlock(blocks.get(25), 1, 100, true));
        assertThrows(UnknownBlockException.class, () -> blockchain.processBlockVote(blocks.get(25), 1));
    }

    // Tests that correct exceptions are thrown when a bad vote occurs
    @Test
    public void testBadVote() {
        Blockchain blockchain = new InMemoryBlockchain(0, 4);
        doTest(first7BlocksIdealNetworkNotarizationThreshold4, blockchain);
        assertThrows(UnknownBlockException.class, () -> blockchain.processBlockVote(blocks.get(25), 1));
    }

    private void doTest(String test, Blockchain blockchain) {
        List<String> commands = test.lines().toList();
        int epoch = -1;
        for (String command : commands) {
            if (command.isEmpty()) continue; // allow blank lines, for readability

            String[] tokens = command.split(" ");
            String firstToken = tokens[0];

            // epoch change command
            if (firstToken.charAt(0) == 'e') {
                epoch = Integer.parseInt(firstToken.substring(1, firstToken.length()-1));
            }

            // node action command
            else if (firstToken.charAt(0) == 'n') {
                int nodeId = Integer.parseInt(firstToken.substring(1));
                String action = tokens[1];
                int blockIndex = Integer.parseInt(tokens[2].substring(1));

                if (action.equals("propose")) {
                    int finalEpoch = epoch;  // for lambda capture
                    assertDoesNotThrow(() -> blockchain.processProposedBlock(blocks.get(blockIndex), nodeId, finalEpoch, true));
                } else if (action.equals("vote")) {
                    assertDoesNotThrow(() -> blockchain.processBlockVote(blocks.get(blockIndex), nodeId));
                } else {
                    throw new IllegalArgumentException("Cannot parse command: " + command);
                }
            }

            // assertion command
            else if (firstToken.equals("assert")) {
                String secondToken = tokens[1];
                int blockIndex = Integer.parseInt(tokens[2].substring(1));
                int parentIndex = tokens.length >= 4 ? Integer.parseInt(tokens[3].substring(1)) : -1;

                // switch expressions (vs statements) do not fall through
                switch (secondToken) {
                    case "parent" -> assertEquals(blockchain.getParent(blocks.get(blockIndex)), blocks.get(parentIndex));
                    case "contains" -> assertTrue(blockchain.contains(blocks.get(blockIndex)));
                    case "notarized" -> assertTrue(blockchain.isNotarized(blocks.get(blockIndex)));
                    case "finalized" -> assertTrue(blockchain.isFinalized(blocks.get(blockIndex)));
                    case "!contains" -> assertFalse(blockchain.contains(blocks.get(blockIndex)));
                    case "!notarized" -> assertFalse(blockchain.isNotarized(blocks.get(blockIndex)));
                    case "!finalized" -> assertFalse(blockchain.isFinalized(blocks.get(blockIndex)));
                    default -> throw new IllegalArgumentException("Cannot parse command: " + command);
                }
            }

            // unrecognized command
            else {
                throw new IllegalArgumentException("Cannot parse command: " + command);
            }
        }
    }

    private static byte[] randomPayload() {
        byte[] payload = new byte[ThreadLocalRandom.current().nextInt(2048, 4096)];
        ThreadLocalRandom.current().nextBytes(payload);
        return payload;
    }
}
package com.github.alexandergillon.streamlet.node.blockchain.impl.memory;

import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.AlreadyExistsException;
import com.github.alexandergillon.streamlet.node.blockchain.impl.BlockTree;
import org.junit.jupiter.api.RepeatedTest;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBlockTreeTest {

    // Tests that the underlying block at a node is what we passed in
    @RepeatedTest(50)
    public void testUnderlyingBlock() {
        Block block = TestUtils.getRandomBlock();
        BlockTree blockTree = new InMemoryBlockTree(block, null);

        assertEquals(blockTree.getBlockInfo().getBlock(), block);
    }

    // Tests that children are added correctly
    @RepeatedTest(10)
    public void testChildren() {
        int NUM_CHILDREN = 20;

        Block block = TestUtils.getRandomBlock();
        BlockTree blockTree = new InMemoryBlockTree(block, null);
        ArrayList<Block> children = new ArrayList<>();

        for (int i = 0; i < NUM_CHILDREN; i++) {
            Block child = TestUtils.getRandomBlockWithParent(block.getHash());
            assertDoesNotThrow(() -> blockTree.addChild(child));
            children.add(child);
        }

        assertEquals(NUM_CHILDREN, blockTree.getChildren().size());

        for (Block child : children) {
            assertTrue(containsBlock(blockTree.getChildren(), child));
        }
    }

    // Tests that badly formed children throw an exception on insertion
    @RepeatedTest(50)
    public void testBadChildren() {
        Block parent = TestUtils.getRandomBlock();
        BlockTree blockTree = new InMemoryBlockTree(parent, null);

        Block child = TestUtils.getRandomBlock();
        while (Arrays.equals(child.getParentHash(), parent.getHash())) {
            child = TestUtils.getRandomBlock();
        }

        Block finalChild = child;  // lambda capture variable should be final
        assertThrows(IllegalArgumentException.class, () -> blockTree.addChild(finalChild));
    }

    // Tests general insertion/querying of blocks into the tree
    @RepeatedTest(5)
    public void testInsertion() {
        Block rootBlock = TestUtils.getRandomBlock();
        BlockTree root = new InMemoryBlockTree(rootBlock, null);

        ArrayList<Block> blocksAdded = new ArrayList<>();
        blocksAdded.add(rootBlock);

        int numChildren = ThreadLocalRandom.current().nextInt(3, 10);
        for (int i = 0; i < numChildren; i++) {
            Block child = TestUtils.getRandomBlockWithParent(rootBlock.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        int numToAdd = ThreadLocalRandom.current().nextInt(100, 200);
        for (int i = 0; i < numToAdd; i++) {
            Block parent = blocksAdded.get(ThreadLocalRandom.current().nextInt(0, blocksAdded.size()));
            Block child = TestUtils.getRandomBlockWithParent(parent.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        for (Block blockAdded : blocksAdded) {
            BlockTree found = root.find(blockAdded);
            assertNotEquals(found, null);
            assertEquals(found.getBlockInfo().getBlock(), blockAdded);
            if (found.getParent() == null) {
                assertEquals(found.getBlockInfo().getBlock(), rootBlock);
            } else {
                assertArrayEquals(found.getParent().getBlockInfo().getHash(), blockAdded.getParentHash());
            }

            BlockTree foundByHash = root.findByHash(blockAdded.getHash());
            assertNotEquals(foundByHash, null);
            assertEquals(foundByHash.getBlockInfo().getBlock(), blockAdded);
            if (foundByHash.getParent() == null) {
                assertEquals(foundByHash.getBlockInfo().getBlock(), rootBlock);
            } else {
                assertArrayEquals(foundByHash.getParent().getBlockInfo().getHash(), blockAdded.getParentHash());
            }
        }
    }

    // Tests that repeated addChild() throws correct exception
    @RepeatedTest(20)
    public void testRepeatedAddChild() {
        Block parentBlock = TestUtils.getRandomBlock();
        BlockTree parent = new InMemoryBlockTree(parentBlock, null);

        Block child = TestUtils.getRandomBlockWithParent(parentBlock.getHash());
        assertDoesNotThrow(() -> parent.addChild(child));
        assertThrows(AlreadyExistsException.class, () -> parent.addChild(child));
    }

    // Tests that repeated insert() throws correct exception
    @RepeatedTest(10)
    public void testRepeatedInsertion() {
        Block rootBlock = TestUtils.getRandomBlock();
        BlockTree root = new InMemoryBlockTree(rootBlock, null);

        ArrayList<Block> blocksAdded = new ArrayList<>();
        blocksAdded.add(rootBlock);

        int numChildren = ThreadLocalRandom.current().nextInt(3, 10);
        for (int i = 0; i < numChildren; i++) {
            Block child = TestUtils.getRandomBlockWithParent(rootBlock.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        int numToAdd = ThreadLocalRandom.current().nextInt(100, 200);
        for (int i = 0; i < numToAdd; i++) {
            Block parent = blocksAdded.get(ThreadLocalRandom.current().nextInt(0, blocksAdded.size()));
            Block child = TestUtils.getRandomBlockWithParent(parent.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        for (int i = 0; i < 5; i++) {
            Block alreadyInserted = blocksAdded.get(ThreadLocalRandom.current().nextInt(0, blocksAdded.size()));
            assertThrows(AlreadyExistsException.class, () -> root.insert(alreadyInserted));
        }
    }

    // Tests that bad blocks throw an exception when inserted into the tree
    @RepeatedTest(5)
    public void testBadInsertion() {
        Block rootBlock = TestUtils.getRandomBlock();
        BlockTree root = new InMemoryBlockTree(rootBlock, null);

        ArrayList<Block> blocksAdded = new ArrayList<>();
        blocksAdded.add(rootBlock);

        int numChildren = ThreadLocalRandom.current().nextInt(3, 10);
        for (int i = 0; i < numChildren; i++) {
            Block child = TestUtils.getRandomBlockWithParent(rootBlock.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        int numToAdd = ThreadLocalRandom.current().nextInt(50, 100);
        for (int i = 0; i < numToAdd; i++) {
            Block parent = blocksAdded.get(ThreadLocalRandom.current().nextInt(0, blocksAdded.size()));
            Block child = TestUtils.getRandomBlockWithParent(parent.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        for (int i = 0; i < 10; i++) {
            Block badBlock = TestUtils.getRandomBlock();

            // Get a block which certainly does not have its parent in the tree
            while (true) {
                boolean doBreak = true;
                for (Block blockAdded : blocksAdded) {
                    if (Arrays.equals(blockAdded.getHash(), badBlock.getParentHash())) {
                        doBreak = false;
                        break;
                    }
                }
                if (doBreak) {
                    break;
                } else {
                    badBlock = TestUtils.getRandomBlock();
                }
            }

            Block finalBadBlock = badBlock;  // lambda capture variable should be final
            assertThrows(NoSuchElementException.class, () -> root.insert(finalBadBlock));
        }
    }

    // Tests simple voting (direct voting on a node)
    @RepeatedTest(50)
    public void testSimpleVoting() {
        Block block = TestUtils.getRandomBlock();
        BlockTree blockTree = new InMemoryBlockTree(block, null);
        blockTree.vote(1);
        blockTree.vote(2);
        blockTree.vote(1);
        blockTree.vote(3);
        blockTree.vote(1);
        assertEquals(blockTree.getVotes(), 3);
    }

    // Tests voting (voting on a block somewhere in the tree)
    @RepeatedTest(5)
    public void testVoting() {
        Block rootBlock = TestUtils.getRandomBlock();
        BlockTree root = new InMemoryBlockTree(rootBlock, null);

        ArrayList<Block> blocksAdded = new ArrayList<>();
        blocksAdded.add(rootBlock);

        int numChildren = ThreadLocalRandom.current().nextInt(3, 10);
        for (int i = 0; i < numChildren; i++) {
            Block child = TestUtils.getRandomBlockWithParent(rootBlock.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        int numToAdd = ThreadLocalRandom.current().nextInt(50, 100);
        for (int i = 0; i < numToAdd; i++) {
            Block parent = blocksAdded.get(ThreadLocalRandom.current().nextInt(0, blocksAdded.size()));
            Block child = TestUtils.getRandomBlockWithParent(parent.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        HashMap<String, Integer> hashToVotes = new HashMap<>();

        for (Block blockAdded : blocksAdded) {
            int timesToVote = ThreadLocalRandom.current().nextInt(0, 50);
            for (int i = 0; i < timesToVote; i++) {
                root.voteOnBlock(blockAdded, i);
            }
            hashToVotes.put(blockAdded.getHashBase64(), timesToVote);
        }

        for (Block blockAdded : blocksAdded) {
            assertEquals(hashToVotes.get(blockAdded.getHashBase64()), root.getVotesOnBlock(blockAdded));
        }
    }

    // Tests repeated voting (that the same voter voting multiple times does not cause number of votes to increase)
    @RepeatedTest(10)
    public void testRepeatedVotes() {
        Block rootBlock = TestUtils.getRandomBlock();
        BlockTree root = new InMemoryBlockTree(rootBlock, null);

        ArrayList<Block> blocksAdded = new ArrayList<>();
        blocksAdded.add(rootBlock);

        int numChildren = ThreadLocalRandom.current().nextInt(3, 10);
        for (int i = 0; i < numChildren; i++) {
            Block child = TestUtils.getRandomBlockWithParent(rootBlock.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        int numToAdd = ThreadLocalRandom.current().nextInt(50, 100);
        for (int i = 0; i < numToAdd; i++) {
            Block parent = blocksAdded.get(ThreadLocalRandom.current().nextInt(0, blocksAdded.size()));
            Block child = TestUtils.getRandomBlockWithParent(parent.getHash());
            assertDoesNotThrow(() -> root.insert(child));
            blocksAdded.add(child);
        }

        Block testBlock = blocksAdded.get(ThreadLocalRandom.current().nextInt(0, blocksAdded.size()));
        root.voteOnBlock(testBlock, 1);
        root.voteOnBlock(testBlock, 2);
        root.voteOnBlock(testBlock, 1);
        root.voteOnBlock(testBlock, 3);
        root.voteOnBlock(testBlock, 1);
        assertEquals(root.getVotesOnBlock(testBlock), 3);
    }

    // Helper method for checking whether a block is in a list of BlockTrees
    private static boolean containsBlock(List<? extends BlockTree> list, Block block) {
        for (BlockTree blockTree : list) {
            if (blockTree.getBlockInfo().getBlock().equals(block)) {
                return true;
            }
        }

        return false;
    }



}
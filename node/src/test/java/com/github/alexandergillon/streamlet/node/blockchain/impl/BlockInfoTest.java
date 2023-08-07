package com.github.alexandergillon.streamlet.node.blockchain.impl;

import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class BlockInfoTest {

    // Tests that getters pass straight through to underlying block
    @RepeatedTest(50)
    public void testGetters() {
        Block block = TestUtils.getRandomBlock();
        BlockInfo blockInfo = new BlockInfo(block);

        assertArrayEquals(block.getParentHash(), blockInfo.getParentHash());
        assertEquals(block.getEpoch(), blockInfo.getEpoch());
        assertArrayEquals(block.getPayload(), blockInfo.getPayload());
        assertArrayEquals(block.getHash(), blockInfo.getHash());
    }

    // Tests voting
    @RepeatedTest(50)
    public void testVoters() {
        Block block = TestUtils.getRandomBlock();
        BlockInfo blockInfo = new BlockInfo(block);

        assertEquals(0, blockInfo.getVotes());

        HashSet<Integer> votersAdded = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            int voter = ThreadLocalRandom.current().nextInt(0, 1000);
            blockInfo.addVoter(voter);
            votersAdded.add(voter);
        }

        assertEquals(votersAdded.size(), blockInfo.getVotes());
    }

    // Tests notarization and finalization
    @RepeatedTest(10)
    public void testNotarizedFinalized() {
        Block block1 = TestUtils.getRandomBlock();
        BlockInfo blockInfo1 = new BlockInfo(block1);

        assertFalse(blockInfo1.isNotarized());
        assertFalse(blockInfo1.isFinalized());

        blockInfo1.notarize();

        assertTrue(blockInfo1.isNotarized());
        assertFalse(blockInfo1.isFinalized());

        blockInfo1.finalizeBlock();

        assertTrue(blockInfo1.isNotarized());
        assertTrue(blockInfo1.isFinalized());

        Block block2 = TestUtils.getRandomBlock();
        BlockInfo blockInfo2 = new BlockInfo(block2);

        assertFalse(blockInfo2.isNotarized());
        assertFalse(blockInfo2.isFinalized());

        blockInfo2.finalizeBlock();

        // finalization implies notarization
        assertTrue(blockInfo2.isNotarized());
        assertTrue(blockInfo2.isFinalized());
    }

}
package com.github.alexandergillon.streamlet.node.blockchain;

import com.github.alexandergillon.streamlet.node.TestUtils;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class BlockTest {

    // Tests block equality
    @RepeatedTest(50)
    public void testEquality() {
        byte[] parentHash = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash);
        int epoch = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload = new byte[payloadSize];
        ThreadLocalRandom.current().nextBytes(payload);

        Block block1 = new Block(parentHash, epoch, payload);
        Block block2 = new Block(parentHash, epoch, payload);

        assertEquals(block1, block2);
    }

    // Tests block non-equality when all fields are different
    @RepeatedTest(50)
    public void testNonEqualityAllDifferent() {
        byte[] parentHash1 = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash1);
        int epoch1 = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize1 = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload1 = new byte[payloadSize1];
        ThreadLocalRandom.current().nextBytes(payload1);

        byte[] parentHash2 = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash2);
        int epoch2 = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize2 = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload2 = new byte[payloadSize2];
        ThreadLocalRandom.current().nextBytes(payload2);

        // For the exceedingly rare chance that we randomly generated the same parent hash array twice
        for (int i = 0; i < parentHash1.length; i++) {
            if (parentHash1[i] == parentHash2[i]) {
                parentHash1[i]++;
            } else {
                break;
            }
        }

        if (epoch1 == epoch2) epoch1 += ThreadLocalRandom.current().nextInt(0, 1000);

        // For the exceedingly rare chance that we randomly generated the same payload array twice
        for (int i = 0; i < Integer.min(payloadSize1, payloadSize2); i++) {
            if (payload1[i] == payload2[i]) {
                payload1[i]++;
            } else {
                break;
            }
        }

        Block block1 = new Block(parentHash1, epoch1, payload1);
        Block block2 = new Block(parentHash2, epoch2, payload2);

        assertNotEquals(block1, block2);
    }

    // Tests non-equality when only parent hash is different
    @RepeatedTest(50)
    public void testNonEqualityDifferentParent() {
        byte[] parentHash1 = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash1);
        int epoch1 = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize1 = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload1 = new byte[payloadSize1];
        ThreadLocalRandom.current().nextBytes(payload1);

        byte[] parentHash2 = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash1);

        Block block1 = new Block(parentHash1, epoch1, payload1);
        Block block2 = new Block(parentHash2, epoch1, payload1);

        assertNotEquals(block1, block2);
    }

    // Tests non-equality when only parent epoch is different
    @RepeatedTest(50)
    public void testNonEqualityDifferentEpoch() {
        byte[] parentHash1 = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash1);
        int epoch1 = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize1 = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload1 = new byte[payloadSize1];
        ThreadLocalRandom.current().nextBytes(payload1);

        int epoch2 = ThreadLocalRandom.current().nextInt(0, 1_000_000);

        Block block1 = new Block(parentHash1, epoch1, payload1);
        Block block2 = new Block(parentHash1, epoch2, payload1);

        assertNotEquals(block1, block2);
    }

    // Tests non-equality when only payload is different
    @RepeatedTest(50)
    public void testNonEqualityDifferentPayload() {
        byte[] parentHash1 = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash1);
        int epoch1 = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize1 = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload1 = new byte[payloadSize1];
        ThreadLocalRandom.current().nextBytes(payload1);

        int payloadSize2 = ThreadLocalRandom.current().nextInt(0, 4096);
        while (payloadSize1 == payloadSize2) {
            payloadSize2 = ThreadLocalRandom.current().nextInt(0, 4096);
        }
        byte[] payload2 = new byte[payloadSize2];
        ThreadLocalRandom.current().nextBytes(payload2);

        byte[] payload3 = new byte[payloadSize1];
        ThreadLocalRandom.current().nextBytes(payload3);
        while (Arrays.equals(payload1, payload3)) {
            ThreadLocalRandom.current().nextBytes(payload3);
        }

        Block block1 = new Block(parentHash1, epoch1, payload1);
        Block block2 = new Block(parentHash1, epoch1, payload2);
        Block block3 = new Block(parentHash1, epoch1, payload3);

        assertNotEquals(block1, block2);
        assertNotEquals(block1, block3);
        assertNotEquals(block2, block3);
    }

    // Tests block serialization
    @RepeatedTest(50)
    public void testSerialization() {
        byte[] parentHash = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash);
        int epoch = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload = new byte[payloadSize];
        ThreadLocalRandom.current().nextBytes(payload);

        Block block = new Block(parentHash, epoch, payload);

        byte[] serialization = block.toBytes();

        assertEquals(serialization.length, parentHash.length+2*TestUtils.INT_SIZE_BYTES+payloadSize);

        assertTrue(Arrays.equals(serialization, 0, parentHash.length, parentHash, 0, parentHash.length));
        assertEquals(epoch, TestUtils.byteArrayToIntBigEndian(Arrays.copyOfRange(serialization, parentHash.length, parentHash.length+TestUtils.INT_SIZE_BYTES)));
        assertEquals(payloadSize, TestUtils.byteArrayToIntBigEndian(Arrays.copyOfRange(serialization, parentHash.length+TestUtils.INT_SIZE_BYTES, parentHash.length+2*TestUtils.INT_SIZE_BYTES)));
        assertTrue(Arrays.equals(serialization, parentHash.length+2*TestUtils.INT_SIZE_BYTES, parentHash.length+2*TestUtils.INT_SIZE_BYTES+payloadSize, payload, 0, payloadSize));
    }

    // Tests that block does not take our buffers, but makes an internal copy
    @Test
    public void testBufferCopy() {
        byte[] parentHash = new byte[TestUtils.SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash);
        int epoch = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload = new byte[payloadSize];
        ThreadLocalRandom.current().nextBytes(payload);

        byte[] parentHashBefore = parentHash.clone();
        byte[] payloadBefore = payload.clone();
        Block block = new Block(parentHash, epoch, payload);
        ThreadLocalRandom.current().nextBytes(parentHash);
        ThreadLocalRandom.current().nextBytes(payload);

        assertArrayEquals(parentHashBefore, block.getParentHash());
        assertArrayEquals(payloadBefore, block.getPayload());
    }
}
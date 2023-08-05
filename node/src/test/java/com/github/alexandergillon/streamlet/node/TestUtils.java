package com.github.alexandergillon.streamlet.node;

import com.github.alexandergillon.streamlet.node.blockchain.Block;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

// Test utilities
public class TestUtils {

    // Utility class
    private TestUtils() { }

    public static final int INT_SIZE_BYTES = 4;
    public static final int SHA_256_HASH_LENGTH_BYTES = 256 / 8;  // 256 bits

    // De-serializes big-endian ints
    public static int byteArrayToIntBigEndian(byte[] bytes) {
        if (bytes.length != INT_SIZE_BYTES) throw new IllegalArgumentException("Input must be 4 bytes.");
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    // Gets a random block
    public static Block getRandomBlock() {
        byte[] parentHash = new byte[SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash);
        int epoch = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload = new byte[payloadSize];
        ThreadLocalRandom.current().nextBytes(payload);

        return new Block(parentHash, epoch, payload);
    }

    // Gets a random block, with a specific parent hash
    public static Block getRandomBlockWithParent(byte[] parentHash) {
        if (parentHash.length != SHA_256_HASH_LENGTH_BYTES) throw new IllegalArgumentException("Parent hash is not SHA256.");
        int epoch = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        int payloadSize = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload = new byte[payloadSize];
        ThreadLocalRandom.current().nextBytes(payload);

        return new Block(parentHash, epoch, payload);
    }

}
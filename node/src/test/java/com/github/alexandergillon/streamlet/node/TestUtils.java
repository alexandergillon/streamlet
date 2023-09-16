/*
 * Copyright (C) 2023 Alexander Gillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.alexandergillon.streamlet.node;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.PayloadMessage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
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

    // Gets a random block with a readable payload
    public static Block getRandomReadableBlock() {
        byte[] parentHash = new byte[SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash);
        int epoch = ThreadLocalRandom.current().nextInt(0, 1_000_000);

        return new Block(parentHash, epoch, randomMessage().toStringBytes());
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

    // Gets a random block, with a specific parent hash and specific epoch
    public static Block getRandomBlockWithParentAndEpoch(Block parent, int epoch) {
        if (parent.getHash().length != SHA_256_HASH_LENGTH_BYTES) throw new IllegalArgumentException("Parent hash is not SHA256.");
        int payloadSize = ThreadLocalRandom.current().nextInt(0, 4096);
        byte[] payload = new byte[payloadSize];
        ThreadLocalRandom.current().nextBytes(payload);

        return new Block(parent.getHash(), epoch, payload);
    }

    // Gets a random payload for a block
    public static byte[] randomPayload() {
        byte[] payload = new byte[ThreadLocalRandom.current().nextInt(2048, 4096)];
        ThreadLocalRandom.current().nextBytes(payload);
        return payload;
    }

    // Gets a random block with a specified payload
    public static Block getRandomBlockWithPayload(byte[] payload) {
        byte[] parentHash = new byte[SHA_256_HASH_LENGTH_BYTES];
        ThreadLocalRandom.current().nextBytes(parentHash);
        int epoch = ThreadLocalRandom.current().nextInt(0, 1_000_000);

        return new Block(parentHash, epoch, payload);
    }

    // Gets a random message
    public static PayloadMessage randomMessage() {
        return new PayloadMessage(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                System.currentTimeMillis() - ThreadLocalRandom.current().nextInt(0, 6000000));
    }

}

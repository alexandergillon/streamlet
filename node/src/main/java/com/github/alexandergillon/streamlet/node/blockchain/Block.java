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
package com.github.alexandergillon.streamlet.node.blockchain;

import com.github.alexandergillon.streamlet.node.models.JsonBlock;
import com.github.alexandergillon.streamlet.node.util.SerializationUtils;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

/** Object which represents a block on the blockchain. */
@Getter
public class Block {

    /** Length of a SHA256 hash in bytes. SHA256 is 256 bits. */
    public static final int SHA_256_HASH_LENGTH_BYTES = 256 / 8;

    /** SHA256 hash of the parent block in the blockchain. */
    private final byte[] parentHash;
    /** Epoch number of this block. */
    private final int epoch;
    /** Payload of this block. */
    private final byte[] payload;
    /** SHA256 hash of the block. */
    private final byte[] hash;

    /**
     * Constructor.
     *
     * @throws IllegalArgumentException If the parent hash is not a SHA256 hash.
     */
    public Block(byte[] parentHash, int epoch, byte[] payload) throws IllegalArgumentException {
        if (parentHash.length != SHA_256_HASH_LENGTH_BYTES) throw new IllegalArgumentException("Parent hash of block being constructed is not SHA256.");
        this.parentHash = parentHash.clone();
        this.epoch = epoch;
        this.payload = payload.clone();

        hash = calculateSha256Hash();
    }

    /** The genesis block. */
    public static final Block GENESIS_BLOCK = new Block(new byte[SHA_256_HASH_LENGTH_BYTES], 0, new byte[0]);

    /**
     * Serializes this block, in the following format:                    <pre>
     *   byte[32]                parent hash
     *   int32                   epoch
     *   int32                   payload length
     *   byte[payload length]    payload                                  </pre>
     *
     *
     * @return This block, serialized.
     */
    public byte[] toBytes() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(parentHash);
            outputStream.write(SerializationUtils.intToFourBytesBigEndian(epoch));
            outputStream.write(SerializationUtils.intToFourBytesBigEndian(payload.length));
            outputStream.write(payload);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("ByteArrayOutputStream cannot throw IOException.", e);
        }
    }

    /** @return This block, as a {@link JsonBlock}. */
    public JsonBlock toJsonBlock() {
        return new JsonBlock(getParentHashBase64(), epoch, getPayloadBase64());
    }

    /** @return The parent hash of this block, as a base-64 encoded string. */
    public String getParentHashBase64() {
        return Base64.getEncoder().encodeToString(parentHash);
    }

    /** @return The hash of this block, as a base-64 encoded string. */
    public String getHashBase64() {
        return Base64.getEncoder().encodeToString(hash);
    }

    /** @return The payload of this block, as a base-64 encoded string. */
    public String getPayloadBase64() {
        return Base64.getEncoder().encodeToString(payload);
    }

    /** @return The SHA256 hash of this block, where this block is serialized as in toBytes(). */
    private byte[] calculateSha256Hash() {
        try {
            byte[] bytes = toBytes();
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No SHA-256 algorithm provider.");
        }
    }

    /**
     * Compares this block for equality with another object. Block hashes are used for performance.
     *
     * @param o The object to compare this block to.
     * @return Whether that object is equal to this block.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return Arrays.equals(hash, block.hash);
    }

    /** @return An integer hash for this block. */
    @Override
    public int hashCode() {
        return Arrays.hashCode(hash);
    }

    @Override
    public String toString() {
        return "Block{" +
                "parentHash=" + getParentHashBase64() +
                ", epoch=" + epoch +
                ", payload=" + getPayloadBase64() +
                ", hash=" + getHashBase64() +
                '}';
    }
}

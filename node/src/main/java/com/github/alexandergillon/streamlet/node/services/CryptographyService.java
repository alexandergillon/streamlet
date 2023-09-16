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
package com.github.alexandergillon.streamlet.node.services;

import com.github.alexandergillon.streamlet.node.blockchain.Block;

/** Service which implements cryptography related functionality. */
public interface CryptographyService {

    /**
     * Signs a block with this node's private key. The block is serialized as in {@link Block#toBytes()}, then
     * signed with SHA384withECDSA.
     *
     * @param block The block to sign.
     * @return A digital signature of the block with SHA384withECDSA and this node's private key.
     */
    byte[] sign(Block block);

    /** Same as {@link #sign(Block)}, but returns the signature as a base-64 encoded string. */
    String signBase64(Block block);

    /**
     * Validates that a digital signature on a block came from the proposer of that block.
     *
     * @param block The block to validate.
     * @param signature The digital signature to validate.
     * @return Whether the digital signature is a valid digital signature from the proposer of the block.
     */
    boolean validateProposal(Block block, byte[] signature);

    /**
     * Validates that a digital signature on a block came from a specific node.
     *
     * @param block The block to validate.
     * @param voter The ID of the node who voted on the block.
     * @param signature The digital signature to validate.
     * @return Whether the digital signature is a valid digital signature from the voter, on the block.
     */
    boolean validateVote(Block block, int voter, byte[] signature);

    /**
     * Returns the leader for a given epoch. The leader for an epoch is given by the following process:
     *
     * <ul>
     * <li>Convert the epoch to 4 bytes (big endian)</li>
     * <li>Take a SHA256 of the bytes</li>
     * <li>Take the first 4 bytes of the hash and convert to an integer (big endian)</li>
     * <li>Take that integer modulo the number of nodes in the network</li>
     * </ul>
     *
     * @param epoch An epoch.
     * @return The ID of the node who is leader during that epoch.
     */
    int leaderForEpoch(int epoch);

}

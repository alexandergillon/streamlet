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

import java.util.List;

/**
 * Service which handles interaction with the blockchain. Note: in order to function correctly,
 * implementers of this service must be kept synchronized with the {@link #setEpoch(int)} function.
 */
public interface BlockchainService {

    /**
     * Sets the current epoch. In order for this service to function correctly, this must be called
     * in a timely fashion. NOTE: internally, the epoch starts in an invalid state. Epoch must be set before
     * calling other functions.
     *
     * @param epoch The current epoch to set the epoch to.
     * @throws IllegalArgumentException If epoch is less than zero. Also, if the epoch is less than or equal to the
     * current epoch. Epoch should only increase.
     */
    void setEpoch(int epoch) throws IllegalArgumentException;

    /**
     * Processes a 'propose block' message from another node. The block is validated, and added to the blockchain
     * if valid. Returns whether this node should vote on the block, according to the Streamlet protocol. <br> <br>
     *
     * If the block is invalid (e.g. signature does not match), logs and discards the block.
     *
     * @param block The proposed block.
     * @param proposer The node who proposed the block.
     * @param signature The digital signature of the proposer on the block.
     * @return Whether this block should be voted on by this node, according to the Streamlet protocol.
     */
    boolean processProposedBlock(Block block, int proposer, byte[] signature);

    /**
     * Processes a 'propose block' message from another node. The vote is validated, and tallied if valid. <br> <br>
     *
     * If the vote is invalid (e.g. signature does not match), logs and discards the block.
     *
     * @param block The voted-on block.
     * @param voterId ID of the node who voted on the block.
     * @param signature The digital signature of the voter on the block.
     * @param proposerSignature The digital signature of the original proposer on the block.
     */
    void processBlockVote(Block block, int voterId, byte[] signature, byte[] proposerSignature);

    /** @return The finalized chain of the blockchain, from oldest to youngest block. */
    List<Block> getFinalizedChain();

    void proposeBlock();

}

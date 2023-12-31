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

import com.github.alexandergillon.streamlet.node.blockchain.exceptions.InvalidBlockException;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.UnknownBlockException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/** Interface to be implemented by a blockchain which implements the Streamlet protocol. */
public interface Blockchain {

    /**
     * Processes a 'propose block' message from another node. The block must be validated before calling this function
     * (the proposer is really the leader for the epoch, the signature is valid, etc.).
     *
     * @param block         The block that was proposed.
     * @param proposer      The node who proposed the block.
     * @param currentEpoch  The current epoch.
     * @param firstProposal Whether this proposed block is the first proposal from this proposer in this epoch.
     * @return Whether this node in the network should vote on this block, according to the Streamlet protocol.
     * @throws InvalidBlockException If the block is invalid. This exception is always thrown if the proposed block
     * has an epoch less than or equal to its parent's epoch. This exception may also be thrown if the block is invalid
     * in some other way, but there is no guarantee that this exception will always be thrown in a block is invalid.
     * @throws UnknownBlockException If the block's parent is not found in the tree.
     */
    boolean processProposedBlock(Block block, int proposer, int currentEpoch, boolean firstProposal) throws InvalidBlockException, UnknownBlockException;

    /**
     * Processes a 'vote' message from another node. The block must be validated before calling this function
     * (the signature is valid, etc.).
     *
     * @param block The block that was voted on.
     * @param voterId The voter who voted on the block.
     * @throws InvalidBlockException If the block is invalid. Note - there is no guarantee that this exception will be
     * thrown in a block is invalid. Only that if this exception is thrown, the block is invalid.
     * @throws UnknownBlockException If the block (or its parent) cannot be found in the tree.
     */
    void processBlockVote(Block block, int voterId) throws InvalidBlockException, UnknownBlockException;

    /**
     * Queries whether a block is contained within the blockchain.
     *
     * @param block The block to query.
     * @return Whether that block is contained within the blockchain.
     */
    boolean contains(Block block);

    /**
     * Queries whether a block in the blockchain is notarized.
     *
     * @param block The block to query.
     * @return Whether that block is notarized.
     * @throws NoSuchElementException If the block does not exist in the blockchain.
     */
    boolean isNotarized(Block block) throws NoSuchElementException;

    /**
     * Queries whether a block in the blockchain is finalized.
     *
     * @param block The block to query.
     * @return Whether that block is finalized.
     * @throws NoSuchElementException If the block does not exist in the blockchain.
     */
    boolean isFinalized(Block block) throws NoSuchElementException;

    /**
     * Gets the parent block of a block in the blockchain.
     * @param block The block whose parent to find.
     * @return The parent of that block, or {@code null} if the block is the genesis block.
     * @throws NoSuchElementException If the block does not exist in the blockchain.
     */
    Block getParent(Block block) throws NoSuchElementException;

    /** @return The finalized chain of the blockchain, from oldest to youngest block. */
    List<Block> getFinalizedChain();

    /**
     * Gets the tail block of the longest notarized chain of the blockchain. This is the block that should be the
     * parent of any block proposed by this node.
     *
     * @return The tail block of the longest notarized chain of the blockchain.
     */
    Block getLongestNotarizedChainTail();

    /**
     * Gets the set of unfinalized blocks ending at a specific block. For example, if the following is the blockchain: <br> <br>
     *
     * {@code GENESIS** --- b0** --- b1** --- b2* --- b3* --- b4* --- b5 --- b6} <br> <br>
     *
     * Where {@code *} means that a block is notarized, and {@code **} means that a block is notarized and finalized,
     * then getUnfinalizedAncestorSetOf(b4) would return {b2, b3, b4}. <br> <br>
     *
     * A note on the function name: blocks are considered ancestors of themselves.
     *
     * @param block The block whose unfinalized ancestor set to get.
     * @return The unfinalized ancestor set of that block.
     * @throws IllegalArgumentException If the block is not in the blockchain.
     */
    Set<Block> getUnfinalizedAncestorSetOf(Block block) throws IllegalArgumentException;
}

package com.github.alexandergillon.streamlet.node.blockchain;

import com.github.alexandergillon.streamlet.node.blockchain.exceptions.InvalidBlockException;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.UnknownBlockException;

public interface Blockchain {

    /**
     * Processes a 'propose block' message from another node. The block must be validated before calling this function
     * (the proposer is really the leader for the epoch, the signature is valid, etc.).
     *
     * @param block The block that was proposed.
     * @param proposer The node who proposed the block.
     * @param currentEpoch The current epoch.
     * @throws InvalidBlockException If the block is invalid.
     * @throws UnknownBlockException If the block's parent is not found in the tree.
     */
    void processProposedBlock(Block block, int proposer, int currentEpoch) throws InvalidBlockException, UnknownBlockException;
    /**
     * Processes a 'vote' message from another node. The block must be validated before calling this function
     * (the signature is valid, etc.).
     *
     * @param block The block that was voted on.
     * @param voterId The voter who voted on the block.
     * @param currentEpoch The current epoch.
     * @throws InvalidBlockException If the block is invalid.
     * @throws UnknownBlockException If the block (or its parent) cannot be found in the tree.
     */
    void processBlockVote(Block block, int voterId, int currentEpoch) throws InvalidBlockException, UnknownBlockException;

}

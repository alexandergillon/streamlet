package com.github.alexandergillon.streamlet.node.blockchain.impl;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/** Stores a block, and information about it (such as voters on the block, whether the block is notarized/finalized, etc.). */
public class BlockInfo {

    /** The block whose information we are tracking. */
    @Getter
    private final Block block;
    /** Whether the block is notarized. */
    private boolean notarized = false;
    /** Whether the block is finalized. */
    private boolean finalized = false;
    /** Who has voted on the block. */
    private final Set<Integer> voters = new HashSet<>();

    /** Constructor. */
    public BlockInfo(Block block) {
        this.block = block;
    }

    /** Convenience method to get the parent hash of the underlying block. */
    public byte[] getParentHash() {
        return block.getParentHash();
    }

    /** Convenience method to get the epoch number of the underlying block. */
    public int getEpoch() {
        return block.getEpoch();
    }

    /** Convenience method to get the payload of the underlying block. */
    public byte[] getPayload() {
        return block.getPayload();
    }

    /** Convenience method to get the hash of the underlying block. */
    public byte[] getHash() {
        return block.getHash();
    }

    /**
     * Adds a specific voter to the set of voters on this block. If the voter with {@code voterId} has already
     * voted on this block, this has no effect.
     *
     * @param voterId The voter who voted on this block.
     */
    public void addVoter(int voterId) {
        voters.add(voterId);
    }

    /** @return The number of voters who have voted on this block. */
    public int getVotes() {
        return voters.size();
    }

    /** Notarizes this block. */
    public void notarize() {
        notarized = true;
    }

    /** @return Whether this block is notarized. */
    public boolean isNotarized() {
        return notarized;
    }

    /** Finalizes this block. This isn't called {@code finalize()} as this method is already implemented by {@code Object}.*/
    public void finalizeBlock() {
        finalized = true;
    }

    /** @return Whether this block is finalized. */
    public boolean isFinalized() {
        return finalized;
    }
}

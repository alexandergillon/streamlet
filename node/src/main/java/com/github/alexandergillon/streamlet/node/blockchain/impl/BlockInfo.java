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
package com.github.alexandergillon.streamlet.node.blockchain.impl;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.services.PayloadService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/** Stores a block, and information about it (such as voters on the block, whether the block is notarized/finalized, etc.). */
@Slf4j
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
        log.info("Notarized block: {}", block.toString());
        notarized = true;
    }

    /** @return Whether this block is notarized. */
    public boolean isNotarized() {
        return notarized;
    }

    /**
     * Finalizes this block. Also notarizes it, as finalization implies notarization. This isn't called
     * {@code finalize()} as this method is already implemented by {@code Object}.
     *
     * @param payloadService Any payload service that is interested in the finalization of a block. If this argument
     *                       is not null, this payload service will be informed that this block has been finalized.
     */
    public void finalizeBlock(PayloadService payloadService) {
        log.info("Finalized block: {}", block.toString());
        notarized = true;
        finalized = true;
        if (payloadService != null) payloadService.finalizedPayload(block.getPayload());
    }

    /** @return Whether this block is finalized. */
    public boolean isFinalized() {
        return finalized;
    }
}

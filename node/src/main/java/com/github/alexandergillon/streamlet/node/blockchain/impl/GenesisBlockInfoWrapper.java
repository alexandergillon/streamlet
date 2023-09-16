package com.github.alexandergillon.streamlet.node.blockchain.impl;

import com.github.alexandergillon.streamlet.node.blockchain.Block;

/** Wrapper around {@link BlockInfo} for the genesis block, so that it always returns the maximum number of votes. */
public class GenesisBlockInfoWrapper extends BlockInfo {

    public GenesisBlockInfoWrapper() {
        super(Block.GENESIS_BLOCK);
        super.notarize();
        super.finalizeBlock(null);
    }

    @Override
    public int getVotes() {
        return Integer.MAX_VALUE;
    }

}

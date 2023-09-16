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

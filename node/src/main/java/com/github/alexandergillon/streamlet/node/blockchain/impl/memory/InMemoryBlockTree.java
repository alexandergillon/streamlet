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
package com.github.alexandergillon.streamlet.node.blockchain.impl.memory;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.AlreadyExistsException;
import com.github.alexandergillon.streamlet.node.blockchain.impl.BlockInfo;
import com.github.alexandergillon.streamlet.node.blockchain.impl.BlockTree;
import com.github.alexandergillon.streamlet.node.blockchain.impl.GenesisBlockInfoWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/** Implementation of a tree data structure in memory, with a {@link BlockInfo} as the data at each node. */
public class InMemoryBlockTree implements BlockTree {

    /** The data stored at this node. */
    private final BlockInfo blockInfo;
    /** The parent of this node. */
    private final BlockTree parent;
    /** The children of this node. */
    private final List<InMemoryBlockTree> children = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param block The data to be stored at this node.
     * @param parent The parent of this node, or null if this node is the root.
     */
    public InMemoryBlockTree(Block block, BlockTree parent) {
        blockInfo = new BlockInfo(block);
        this.parent = parent;
    }

    private InMemoryBlockTree(BlockInfo blockInfo) {
        this.blockInfo = blockInfo;
        parent = null;
    }

    public static InMemoryBlockTree GENESIS_BLOCK_TREE() {
        return new InMemoryBlockTree(new GenesisBlockInfoWrapper());
    }

    @Override
    public BlockInfo getBlockInfo() {
        return blockInfo;
    }

    @Override
    public BlockTree getParent() {
        return parent;
    }

    @Override
    public List<? extends BlockTree> getChildren() {
        return children;
    }

    @Override
    public BlockTree addChild(Block block) throws IllegalArgumentException, AlreadyExistsException {
        if (!Arrays.equals(block.getParentHash(), blockInfo.getHash())) throw new IllegalArgumentException("Parent hash of child to add does not match this node's hash.");

        BlockTree existingChild = searchList(children, block);
        if (existingChild != null) throw new AlreadyExistsException(existingChild, "Block already exists as a child of this node.");

        InMemoryBlockTree child = new InMemoryBlockTree(block, this);
        children.add(child);
        return child;
    }

    @Override
    public BlockTree find(Block block) {
        return findByHash(block.getHash());
    }

    @Override
    public BlockTree findByHash(byte[] hash) {
        if (Arrays.equals(this.blockInfo.getHash(), hash)) {
            return this;
        }

        for (InMemoryBlockTree child : children) {
            BlockTree found = child.findByHash(hash);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    @Override
    public BlockTree insert(Block block) throws NoSuchElementException, AlreadyExistsException {
        if (block.equals(blockInfo.getBlock())) throw new AlreadyExistsException(this, "Block already exists in the tree.");
        BlockTree parent = findByHash(block.getParentHash());
        if (parent == null) throw new NoSuchElementException("Parent block does not exist in tree.");
        return parent.addChild(block);
    }

    @Override
    public void vote(int voterId) {
        blockInfo.addVoter(voterId);
    }

    @Override
    public void voteOnBlock(Block block, int voterId) throws NoSuchElementException {
        BlockTree blockTree = find(block);
        if (blockTree == null) throw new NoSuchElementException("Block does not exist in tree.");
        blockTree.getBlockInfo().addVoter(voterId);
    }

    @Override
    public int getVotes() {
        return blockInfo.getVotes();
    }

    @Override
    public int getVotesOnBlock(Block block) throws NoSuchElementException {
        BlockTree blockTree = find(block);
        if (blockTree == null) throw new NoSuchElementException("Block does not exist in tree.");
        return blockTree.getVotes();
    }

    @Override
    public int getNotarizedChainLength() {
        if (!blockInfo.isNotarized()) return 0;
        if (parent == null) return 1;  // We are the root, and also notarized

        /* We are not the root, hence if our parent chain has length 0, then it is not a notarized chain. Then this
        node is not a part of a notarized chain, and we should return 0. Otherwise, if the parent chain has length
         > 0, then we are a part of a notarized chain, and if we got here, we are also notarized. Hence +1 and return.*/
        int parentChainLength = parent.getNotarizedChainLength();
        return (parentChainLength == 0) ? 0 : 1 + parentChainLength;
    }

    @Override
    public int getLongestNotarizedChainLength() {
        // If any node along the chain is not notarized, that breaks the chain
        if (!blockInfo.isNotarized()) return 0;

        if (children.isEmpty()) {
            return 1;  // This node is notarized from the earlier check
        } else {
            int longestNotarizedChildChain = 0;
            for (BlockTree child : children) {
                longestNotarizedChildChain = Integer.max(longestNotarizedChildChain, child.getLongestNotarizedChainLength());
            }
            return 1 + longestNotarizedChildChain;
        }
    }

    @Override
    public BlockTree getLongestNotarizedChainTail() {
        LongestNotarizedChainResult result = getLongestNotarizedChainTailInternal();
        return result == null ? null : result.bestNode;
    }

    /**
     * Searches a {@link BlockTree} list for a node with a specific {@link Block}.
     *
     * @param list The list of nodes to search.
     * @param block A block to search for.
     * @return The {@link BlockTree} in the list that contains that {@link Block} as a node, or null if none exist.
     */
    private static BlockTree searchList(List<? extends BlockTree> list, Block block) {
        for (BlockTree blockTree : list) {
            if (blockTree.getBlockInfo().getBlock().equals(block)) {
                return blockTree;
            }
        }

        return null;
    }

    /** POJO for getLongestNotarizedChainTailInternal() return value. Essentially just a pair of int, BlockTree. */
    private record LongestNotarizedChainResult(int bestLength, BlockTree bestNode) { }

    /**
     * Gets the longest notarized chain tail from this node, and the length of the notarized chain. If this
     * node is not notarized, returns null.
     *
     * @return The longest notarized chain tail from this node, and the length of that chain, or null if this
     * node is not notarized.
     */
    private LongestNotarizedChainResult getLongestNotarizedChainTailInternal() {
        if (!blockInfo.isNotarized()) return null;

        int bestLength = 0;
        BlockTree bestNode = this;
        for (InMemoryBlockTree child : children) {
            LongestNotarizedChainResult result = child.getLongestNotarizedChainTailInternal();
            if (result == null) continue;

            if (result.bestLength > bestLength) {
                bestLength = result.bestLength;
                bestNode = result.bestNode;
            }
        }

        bestLength++;  // include this node in the count
        return new LongestNotarizedChainResult(bestLength, bestNode);
    }
}

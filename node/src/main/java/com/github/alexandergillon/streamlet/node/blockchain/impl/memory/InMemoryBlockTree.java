package com.github.alexandergillon.streamlet.node.blockchain.impl.memory;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.blockchain.impl.BlockInfo;
import com.github.alexandergillon.streamlet.node.blockchain.impl.BlockTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/** Implementation of a tree data structure, with a {@link BlockInfo} as the data at each node. */
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
    public BlockTree addChild(Block block) throws IllegalArgumentException {
        if (!Arrays.equals(block.getParentHash(), blockInfo.getHash())) throw new IllegalArgumentException("Parent hash of child to add does not match this node's hash.");
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
    public BlockTree insert(Block block) throws NoSuchElementException {
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
}

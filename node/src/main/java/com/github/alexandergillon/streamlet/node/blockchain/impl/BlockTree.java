package com.github.alexandergillon.streamlet.node.blockchain.impl;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.AlreadyExistsException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Interface to be implemented by a 'block tree' - the underlying data structure which stores the structure of the
 * blockchain. At any point, the blockchain is actually a tree, as there may be 'conflicting' (always un-finalized)
 * blocks in the blockchain.
 */
public interface BlockTree {

    /** @return The block info of the block stored at this node. */
    BlockInfo getBlockInfo();

    /** @return The parent of this node, or null if this node is the root. */
    BlockTree getParent();

    /** @return The children of this node. */
    List<? extends BlockTree> getChildren();

    /**
     * Adds a child to this node.
     *
     * @param block The child to add.
     * @return The new node, containing that child as data.
     * @throws IllegalArgumentException If the parent hash of the child to be added does not match this node's hash.
     */
    BlockTree addChild(Block block) throws IllegalArgumentException, AlreadyExistsException;

    /**
     * Finds a block in this tree, if it exists.
     *
     * @param block The block to find.
     * @return The node in this tree which contains that block as data, if one exists, or null otherwise.
     */
    BlockTree find(Block block);

    /**
     * Finds a block in this tree, by its hash.
     *
     * @param hash The hash of the block to find.
     * @return The node in this tree which contains that block as data, if one exists, or null otherwise.
     */
    BlockTree findByHash(byte[] hash);

    /**
     * Inserts a block into this tree, under its parent. Its parent is located using the block's parent hash.
     *
     * @param block The block to insert.
     * @return The new node, containing the inserted block as data.
     * @throws NoSuchElementException If the block's parent does not exist in the tree.
     */
    BlockTree insert(Block block) throws NoSuchElementException, AlreadyExistsException;

    /**
     * Votes on this block.
     *
     * @param voterId The voter who voted on this block.
     */
    void vote(int voterId);

    /**
     * Votes on a block in this tree.
     *
     * @param block The block to vote on. Must be in this tree.
     * @param voterId The voter who voted on the block.
     * @throws NoSuchElementException If the block does not exist in the tree.
     */
    void voteOnBlock(Block block, int voterId) throws NoSuchElementException;

    /** @return The number of votes on the block at this node. */
    int getVotes();

    /**
     * Gets the number of votes on a block in this tree.
     *
     * @param block The block to query.
     * @return The number of votes on that block.
     */
    int getVotesOnBlock(Block block) throws NoSuchElementException;

    /** @return The length of the notarized chain that this node is a part of (may be 0, if this node is not
     * part of a notarized chain). */
    int getNotarizedChainLength();

    /** @return The length of the longest notarized chain in the tree. */
    int getLongestNotarizedChainLength();

}

package com.github.alexandergillon.streamlet.node.blockchain.impl.memory;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.blockchain.Blockchain;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.AlreadyExistsException;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.InvalidBlockException;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.UnknownBlockException;
import com.github.alexandergillon.streamlet.node.blockchain.impl.BlockInfo;
import com.github.alexandergillon.streamlet.node.blockchain.impl.BlockTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/** Implementation of the {@link Blockchain} interface, with data stored in-memory. */
public class InMemoryBlockchain implements Blockchain {

    /**
     * Root of the 'block tree', i.e. the blockchain (which is actually a tree at any given time, due to the
     * possibility of conflicting un-finalized blocks in the blockchain).
     */
    private final BlockTree root = InMemoryBlockTree.GENESIS_BLOCK_TREE();
    /** Node id of this node in the network. */
    private final int networkNodeId;
    /** Threshold for a block to be notarized. */
    private final int notarizationThreshold;
    /** Reference to the latest (in terms of epoch number) block that has been finalized in the blockchain. */
    private BlockTree latestFinalizedBlock = root;  // TODO: use

    /**
     * Constructor.
     *
     * @param networkNodeId The id of this node in the network.
     */
    public InMemoryBlockchain(int networkNodeId, int notarizationThreshold) {
        this.networkNodeId = networkNodeId;
        this.notarizationThreshold = notarizationThreshold;
    }

    @Override
    public boolean processProposedBlock(Block block, int proposer, int currentEpoch, boolean firstProposal) throws InvalidBlockException, UnknownBlockException {
        BlockTree parent = root.findByHash(block.getParentHash());
        if (parent == null) throw new UnknownBlockException("Parent block cannot be found in the tree.");

        if (block.getEpoch() < 0) throw new InvalidBlockException("Epoch of block is < 0.");
        else if (block.getEpoch() > currentEpoch) throw new InvalidBlockException("Epoch of block (" + block.getEpoch() + ") is greater than current epoch (" + currentEpoch + ").");

        if (block.getEpoch() <= parent.getBlockInfo().getEpoch()) throw new InvalidBlockException("Epoch of block (" + block.getEpoch() + ") is less than or equal to epoch of parent (" + parent.getBlockInfo().getEpoch() + ").");

        if (block.getEpoch() != currentEpoch) {
            insertIntoTree(block, parent, proposer, false);
            return false;
        }

        if (parent.getNotarizedChainLength() < root.getLongestNotarizedChainLength()) {
            insertIntoTree(block, parent, proposer, false);
            return false;
        }

        // If this is the first proposal this epoch, then we vote for the block.
        insertIntoTree(block, parent, proposer, firstProposal);
        return firstProposal;
    }

    @Override
    public void processBlockVote(Block block, int voterId) throws UnknownBlockException {
        BlockTree blockTree = root.find(block);
        if (blockTree != null) {
            blockTree.vote(voterId);
            checkForNotarization(blockTree);
            return;
        } else {
            BlockTree parent = root.findByHash(block.getParentHash());
            if (parent != null) {
                insertIntoTree(block, parent, voterId, false);
                return;
            }
        }
        throw new UnknownBlockException("Block, and its parent, cannot be found in the tree.");
    }

    @Override
    public boolean contains(Block block) {
        return root.find(block) != null;
    }

    @Override
    public boolean isNotarized(Block block) throws NoSuchElementException {
        BlockTree found = root.find(block);
        if (found == null) throw new NoSuchElementException("Block does not exist in the blockchain.");
        return found.getBlockInfo().isNotarized();
    }

    @Override
    public boolean isFinalized(Block block) throws NoSuchElementException {
        BlockTree found = root.find(block);
        if (found == null) throw new NoSuchElementException("Block does not exist in the blockchain.");
        return found.getBlockInfo().isFinalized();
    }

    @Override
    public Block getParent(Block block) throws NoSuchElementException {
        BlockTree found = root.find(block);
        if (found == null) throw new NoSuchElementException("Block does not exist in the blockchain.");
        return found.getParent().getBlockInfo().getBlock();
    }

    @Override
    public List<Block> getFinalizedChain() {
        ArrayList<Block> finalizedChain = new ArrayList<>();
        finalizedChain.add(root.getBlockInfo().getBlock());
        BlockTree node = root;
        while (true) {
            BlockTree child = findFinalizedChild(node);
            if (child == null) {
                return finalizedChain;
            } else {
                finalizedChain.add(child.getBlockInfo().getBlock());
                node = child;
            }
        }
    }

    @Override
    public Block getLongestNotarizedChainTail() {
        return root.getLongestNotarizedChainTail().getBlockInfo().getBlock();
    }

    @Override
    public Set<Block> getUnfinalizedAncestorSetOf(Block block) {
        HashSet<Block> unfinalizedSet = new HashSet<>();

        BlockTree current = root.find(block);
        while (!current.getBlockInfo().isFinalized()) {
            unfinalizedSet.add(current.getBlockInfo().getBlock());
            current = current.getParent();
        }

        return unfinalizedSet;
    }

    /**
     * Inserts a block into the block tree, adding the vote of the node who we heard about this block from. For example,
     * if we heard about it because a round leader proposed it, then we need to add their vote. Or, if we heard about
     * it because someone voted on it, we need to add their vote. <br> <br>
     *
     * Optionally allows this node to also vote on the block. <br> <br>
     *
     * If the block is already in the tree, then the votes are applied to that block instead, and the block is not
     * inserted another time.
     *
     * @param block The block to insert.
     * @param parent The parent of the block to insert, in the block tree.
     * @param otherVoter The node who informed us of this block, and therefore must be voting on it.
     * @param vote Whether this node should also vote on the block.
     */
    private void insertIntoTree(Block block, BlockTree parent, int otherVoter, boolean vote) {
        BlockTree insertedBlock;
        try {
            insertedBlock = parent.addChild(block);
        } catch (AlreadyExistsException e) {
            insertedBlock = e.getExistingNode();
        }
        insertedBlock.vote(otherVoter);
        if (vote) {
            insertedBlock.vote(networkNodeId);
        }
        checkForNotarization(insertedBlock);
    }

    /**
     * Checks whether a node should be notarized, after a vote has occurred. If notarization occurs, also checks if
     * any blocks can now be finalized.
     *
     * @param node The node to check whether to notarize.
     */
    private void checkForNotarization(BlockTree node) {
        BlockInfo blockInfo = node.getBlockInfo();
        if (blockInfo.isNotarized()) return;

        if (blockInfo.getVotes() >= notarizationThreshold) {
            blockInfo.notarize();
            checkForFinalization(node);
        }
    }

    /**
     * Checks whether a node or surrounding nodes should be finalized, after a vote has occurred.
     *
     * @param node The node to check whether to finalize (also checks surrounding nodes).
     */
    private void checkForFinalization(BlockTree node) {
        List<? extends BlockTree> children = node.getChildren();

        // We need to check grandchildren as certain message delivery orders could mean that we notarized this node
        // after its children and grandchildren (and that this node is the last thing we need to finalize the prefix
        // chain).
        for (BlockTree child : children) {
            List<? extends BlockTree> grandchildren = child.getChildren();
            for (BlockTree grandchild : grandchildren) {
                if (checkForFinalizationEndingAt(grandchild)) {
                    latestFinalizedBlock = grandchild;
                    return;
                }
            }
        }

        for (BlockTree child : children) {
            if (checkForFinalizationEndingAt(child)) {
                latestFinalizedBlock = child;
                return;
            }
        }

        if (checkForFinalizationEndingAt(node)) {
            latestFinalizedBlock = node;
        }
    }

    /**
     * Checks for a finalized chain ending at (and not including) {@code node}. I.e. checks that node, its parent,
     * and its grandparent are all notarized, and that node.epoch == parent.epoch+1 == grandparent.epoch+2. If so,
     * finalizes the appropriate prefix chain (from parent upwards).
     *
     * @param node The node to check for finalization.
     * @return Whether a prefix chain was finalized, ending at (and not including) node.
     */
    private boolean checkForFinalizationEndingAt(BlockTree node) {
        BlockTree parent = node.getParent();
        if (parent == null) return false;

        BlockTree grandparent = parent.getParent();
        if (grandparent == null) return false;

        if (!node.getBlockInfo().isNotarized()) return false;
        if (!parent.getBlockInfo().isNotarized()) return false;
        if (!grandparent.getBlockInfo().isNotarized()) return false;

        int epoch = node.getBlockInfo().getEpoch();
        if (parent.getBlockInfo().getEpoch() != epoch-1) return false;
        if (grandparent.getBlockInfo().getEpoch() != epoch-2) return false;

        finalizePrefixChain(parent);
        return true;
    }

    /**
     * Finalizes the prefix chain of a node (including that node).
     *
     * @param node The node to finalize the prefix chain (includes the node itself).
     */
    private void finalizePrefixChain(BlockTree node) {
        node.getBlockInfo().finalizeBlock();
        while (true) {
            BlockTree parent = node.getParent();
            if (parent == null) return;
            if (parent.getBlockInfo().isFinalized()) return;
            parent.getBlockInfo().finalizeBlock();
            node = parent;
        }
    }

    /**
     * Finds a finalized child of a node, if one exists.
     *
     * @param node Node to query.
     * @return A finalized child of that node, or none if none exist.
     */
    private static BlockTree findFinalizedChild(BlockTree node) {
        for (BlockTree child : node.getChildren()) {
            if (child.getBlockInfo().isFinalized()) {
                return child;
            }
        }
        return null;
    }
}

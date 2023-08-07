package com.github.alexandergillon.streamlet.node.blockchain.exceptions;

import com.github.alexandergillon.streamlet.node.blockchain.impl.BlockTree;
import lombok.Getter;

/** Exception thrown when a block that already exists in the blockchain is inserted into the tree. */
public class AlreadyExistsException extends Exception {

    /** The already existing node in the block tree, which prevented the block from being inserted. */
    @Getter
    private final BlockTree existingNode;

    /**
     * @param existingNode The already existing node in the block tree
     * @param message Exception message.
     */
    public AlreadyExistsException(BlockTree existingNode, String message) {
        super(message);
        this.existingNode = existingNode;
    }
}

package com.github.alexandergillon.streamlet.node.blockchain.exceptions;

/** Exception thrown when a block is invalid. For example, if the block has a smaller epoch than its parent. */
public class InvalidBlockException extends Exception {

    public InvalidBlockException() {
        super();
    }

    public InvalidBlockException(String message) {
        super(message);
    }

    public InvalidBlockException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidBlockException(Throwable cause) {
        super(cause);
    }

}

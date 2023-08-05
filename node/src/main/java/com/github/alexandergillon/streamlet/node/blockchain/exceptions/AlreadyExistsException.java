package com.github.alexandergillon.streamlet.node.blockchain.exceptions;

/** Exception thrown when a block that already exists in the blockchain is inserted into the tree. */
public class AlreadyExistsException extends Exception {

    public AlreadyExistsException() {
        super();
    }

    public AlreadyExistsException(String message) {
        super(message);
    }

    public AlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyExistsException(Throwable cause) {
        super(cause);
    }

}

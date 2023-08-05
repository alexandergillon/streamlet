package com.github.alexandergillon.streamlet.node.blockchain.exceptions;

/**
 * Exception thrown when a block cannot be found in the blockchain. Catchers should initiate a catch-up process by
 * querying other nodes in the network.
 */
public class UnknownBlockException extends Exception {

    public UnknownBlockException() {
        super();
    }

    public UnknownBlockException(String message) {
        super(message);
    }

    public UnknownBlockException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownBlockException(Throwable cause) {
        super(cause);
    }

}

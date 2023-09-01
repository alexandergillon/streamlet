package com.github.alexandergillon.streamlet.node.services;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.Message;

import java.util.List;

/** Service to handle the payload of blocks. Collects pending messages and returns them when needed.
 * Messages are stored and returned in FIFO order. */
public interface PayloadService {

    /**
     * Adds a pending message to the internal message buffer.
     * @param message The message to add.
     */
    void addPendingMessage(Message message);

    /**
     * Notifies this service that a payload has been finalized. This is so that we can discard pending messages in the
     * buffer which have effectively already been 'sent'.
     *
     * @param payload The payload of the block that was finalized.
     */
    void finalizedPayload(byte[] payload);

    /**
     * Gets the next payload for a block to be proposed. This method takes the parent unfinalized chain (i.e. the
     * ancestors of the to-be-proposed block which are not finalized) as an argument, to avoid proposing a payload that
     * has already been included in that chain but has not yet been finalized.
     *
     * @param unfinalizedChain The ancestors of the to-be-proposed block which are not finalized.
     * @return The payload which should be proposed as part of a block that extends this unfinalized chain, or null
     * if there are no pending messages which could be included in the proposed block (i.e. no pending messages at
     * all, or all pending messages are already in the unfinalized chain).
     */
    byte[] getNextPayload(List<Block> unfinalizedChain);

}

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
package com.github.alexandergillon.streamlet.node.services;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.PayloadMessage;

import java.util.Set;

/** Service to handle the payload of blocks. Collects pending messages and returns them when needed.
 * Messages are stored and returned in FIFO order. */
public interface PayloadService {

    /**
     * Adds a pending message to the internal message buffer.
     * @param message The message to add.
     */
    void addPendingMessage(PayloadMessage message);

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
     * @param unfinalizedSet The ancestors of the to-be-proposed block which are not finalized.
     * @return The payload which should be proposed as part of a block that extends this unfinalized chain, or null
     * if there are no pending messages which could be included in the proposed block (i.e. no pending messages at
     * all, or all pending messages are already in the unfinalized chain).
     */
    byte[] getNextPayload(Set<Block> unfinalizedSet);

}

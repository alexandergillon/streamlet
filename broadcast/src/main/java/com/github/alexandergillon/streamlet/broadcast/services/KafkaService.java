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
package com.github.alexandergillon.streamlet.broadcast.services;

import com.github.alexandergillon.streamlet.broadcast.models.BroadcastMessage;

/** Service to handle communication with Kafka (consuming messages and broadcasting them on the appropriate topics). */
public interface KafkaService {

    /**
     * Processes a broadcast message, by broadcasting the message to nodes on the appropriate Kafka topics.
     *
     * @param message The broadcast message to process.
     */
    void processBroadcast(BroadcastMessage message);

    /**
     * Broadcasts a proposed payload to all nodes.
     *
     * @param username The username of the user who sent this message.
     * @param text Message text.
     */
    void broadcastPayload(String username, String text);

}

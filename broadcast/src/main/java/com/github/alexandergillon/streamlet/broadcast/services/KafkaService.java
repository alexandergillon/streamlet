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

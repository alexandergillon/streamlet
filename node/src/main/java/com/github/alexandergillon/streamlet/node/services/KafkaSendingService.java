package com.github.alexandergillon.streamlet.node.services;

/** Service to handle publishing to Kafka topics. */
public interface KafkaSendingService {

    /**
     * Broadcasts a message to the broadcast topic in Kafka, to be picked up by the broadcast
     * server and relayed to other nodes.
     *
     * @param message The message to broadcast.
     */
    void broadcast(String message);

}

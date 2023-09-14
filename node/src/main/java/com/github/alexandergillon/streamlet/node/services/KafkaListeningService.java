package com.github.alexandergillon.streamlet.node.services;

import com.github.alexandergillon.streamlet.node.models.PayloadMessage;
import com.github.alexandergillon.streamlet.node.models.ProposeMessage;
import com.github.alexandergillon.streamlet.node.models.VoteMessage;

/** Service to handle listening to Kafka topics. */
public interface KafkaListeningService {

    /**
     * Processes a payload message from Kafka. This method should be annotated with {@code @KafkaListener}
     * and appropriate annotation parameters to pick up the correct messages from Kafka.
     *
     * @param message The payload message from Kafka, to be processed.
     */
    void processPayload(PayloadMessage message);

    /**
     * Processes a proposal message from Kafka. This method should be annotated with {@code @KafkaListener}
     * and appropriate annotation parameters to pick up the correct messages from Kafka.
     *
     * @param message The proposal message from Kafka, to be processed.
     */
    void processProposal(ProposeMessage message);

    /**
     * Processes a vote message from Kafka. This method should be annotated with {@code @KafkaListener}
     * and appropriate annotation parameters to pick up the correct messages from Kafka.
     *
     * @param message The vote message from Kafka, to be processed.
     */
    void processVote(VoteMessage message);

}

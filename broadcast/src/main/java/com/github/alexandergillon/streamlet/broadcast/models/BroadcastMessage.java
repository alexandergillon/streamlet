package com.github.alexandergillon.streamlet.broadcast.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * POJO to represent a Kafka message that tells this server to broadcast a message to all other nodes but the sender.
 * Messages are sent as JSON over Kafka, and deserialized by Spring.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BroadcastMessage {

    /** Sender of this message. No byzantine behavior allowed. */
    private int sender;

    /** Type of this message: either 'propose' or 'vote'. */
    private String messageType;

    /**
     * Message, as a JSON string. The exact format of that JSON will depend on message type.
     * See kafka.md in docs for details.
     */
    private JsonNode message;

}

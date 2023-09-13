package com.github.alexandergillon.streamlet.broadcast.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** POJO to represent a message, sent by users to be included in the blockchain. */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class PayloadMessage {

    /** The username of the user who sent this message. Must not contain a colon character. */
    private String username;

    /** Message text. */
    private String text;

    /** Timestamp of the message. */
    private long timestamp;

}

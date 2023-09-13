package com.github.alexandergillon.streamlet.broadcast.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** POJO for payload request deserialization by Spring. */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PayloadRequest {

    /** The username of the user who sent this message. */
    private String username;

    /** Message text. */
    private String text;

}

package com.github.alexandergillon.streamlet.node.models;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** POJO to represent a block, in a way that can be serialized to / deserialized from JSON. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class JsonBlock {

    /** SHA256 hash of the parent block in the blockchain, base-64 encoded. */
    private String parentHash;
    /** Epoch number of this block. */
    private int epoch;
    /** Payload of this block, base-64 encoded. */
    private String payload;

}

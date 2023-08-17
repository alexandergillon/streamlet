package com.github.alexandergillon.streamlet.node.models;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * POJO to represent a Kafka message that notifies this node of a vote.
 * Messages are sent as JSON over Kafka, and deserialized by Spring.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VoteMessage {

    /**
     * ID of the node that (claims to have) voted on this block.
     * Note: may be incorrect if a node is Byzantine.
     */
    private int nodeId;
    /** The block being voted on. */
    private JsonBlock block;
    /**
     * The digital signature of the block, by the proposer. Blocks are serialized as detailed
     * in {@link Block#toBytes()}, then a SHA256 digest is taken, and that digest is signed with
     * SHA384withECDSA to obtain this signature.
     */
    private String signature;
    /**
     * The signature of the block, by the original proposer. This is sent with votes so that
     * nodes can easily discard Byzantine votes (votes on blocks that were never actually
     * proposed).
     */
    private String proposerSignature;

}

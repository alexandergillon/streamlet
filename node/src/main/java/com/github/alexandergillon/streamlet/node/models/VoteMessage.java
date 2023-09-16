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
     * The digital signature of the block, by the voter. Blocks are serialized as detailed
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

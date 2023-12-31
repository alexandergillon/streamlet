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
package com.github.alexandergillon.streamlet.node.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.JsonBlock;
import com.github.alexandergillon.streamlet.node.models.PayloadMessage;
import com.github.alexandergillon.streamlet.node.models.ProposeMessage;
import com.github.alexandergillon.streamlet.node.models.VoteMessage;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

/** Utility class to handle serialization / deserialization functionality. */
@Slf4j
public class SerializationUtils {

    // For JSON serialization
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private SerializationUtils() {
        throw new IllegalStateException("Utility class should not be instantiated.");
    }

    /**
     * Converts an integer to 4 bytes, in big-endian order.
     *
     * @param i Integer to convert.
     * @return That integer as 4 bytes in big-endian order.
     */
    public static byte[] intToFourBytesBigEndian(int i) {
        return ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN).putInt(i).array();
    }

    /**
     * Converts 4 bytes to an integer, in big-endian order.
     *
     * @param bytes 4 bytes to convert.
     * @return Those 4 bytes interpreted as a big-endian integer.
     * @throws IllegalArgumentException If the input byte array is not 4 bytes.
     */
    public static int fourBytesToIntBigEndian(byte[] bytes) throws IllegalArgumentException {
        if (bytes.length != Integer.BYTES) throw new IllegalArgumentException("Input must be " + Integer.BYTES + " bytes.");
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    /**
     * Builds a JSON message that can be broadcast, informing other nodes that this node has proposed a block.
     *
     * @param nodeId The ID of this node.
     * @param proposedBlock The block that this node is proposing.
     * @param signature The digital signature of this node on the proposed block, as a base-64 encoded string.
     * @return A JSON message that can be broadcast, informing other nodes that this node has proposed a block.
     */
    public static String buildProposeBroadcast(int nodeId, Block proposedBlock, String signature) {
        try {
            ObjectNode jsonRoot = objectMapper.createObjectNode();

            jsonRoot.put("sender", nodeId);
            jsonRoot.put("messageType", "propose");
            jsonRoot.set("message", buildProposeMessage(nodeId, proposedBlock, signature));

            return objectMapper.writeValueAsString(jsonRoot);
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a JSON message that can be broadcast, informing other nodes that this node has voted on a block.
     *
     * @param nodeId The ID of this node.
     * @param block The block that this node is voting on.
     * @param signature The digital signature of this node on the block, as a base-64 encoded string.
     * @param proposerSignature The digital signature of the original proposer of this block, as a base-64 encoded string.
     * @return A JSON message that can be broadcast, informing other nodes that this node has voted on a block.
     */
    public static String buildVoteBroadcast(int nodeId, Block block, String signature, String proposerSignature) {
        try {
            ObjectNode jsonRoot = objectMapper.createObjectNode();

            jsonRoot.put("sender", nodeId);
            jsonRoot.put("messageType", "vote");
            jsonRoot.set("message", buildVoteMessage(nodeId, block, signature, proposerSignature));

            return objectMapper.writeValueAsString(jsonRoot);
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a list of blocks to a readable text version of the messages it contains.
     *
     * @param blockList A list of blocks.
     * @return A readable text version of the messages the list of blocks contains.
     */
    public static String blockListMessagesToReadableText(List<Block> blockList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Block block : blockList) {
            stringBuilder.append(new String(block.getPayload(), StandardCharsets.US_ASCII));
            stringBuilder.append("\r\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Converts a list of blocks to its JSON representation. This is a JSON array literal with each member being a
     * PayloadMessage.
     *
     * @param blockList A list of blocks.
     * @return The JSON representation of the messages that the list of blocks contains.
     */
    public static String blockListMessagesToJson(List<Block> blockList) {
        try {
            List<PayloadMessage> jsonMessageList = blockList.stream().map(block -> {
                try {
                    return PayloadMessage.fromStringBytes(block.getPayload());
                } catch (ParseException e) {
                    log.error("ParseException", e);
                    throw new RuntimeException(e);
                }
            }).toList();
            return objectMapper.writeValueAsString(jsonMessageList);
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a {@link ProposeMessage} for a specific block, and returns it as a {@link JsonNode}.
     *
     * @param nodeId The ID of this node.
     * @param proposedBlock The proposed block.
     * @param signature The digital signature of this node on the proposed block, as a base-64 encoded string.
     * @return A {@link ProposeMessage} for the proposed block, converted to a {@link JsonNode}.
     */
    private static JsonNode buildProposeMessage(int nodeId, Block proposedBlock, String signature) {
        JsonBlock jsonBlock = proposedBlock.toJsonBlock();
        ProposeMessage proposeMessage = new ProposeMessage(nodeId, jsonBlock, signature);
        return objectMapper.valueToTree(proposeMessage);
    }

    /**
     * Builds a {@link VoteMessage} for a specific block, and returns it as a {@link JsonNode}.
     *
     * @param nodeId The ID of this node.
     * @param block A block.
     * @param signature The digital signature of this node on the block, as a base-64 encoded string.
     * @param proposerSignature The digital signature of the original proposer of the block, as a base-64 encoded string.
     * @return A {@link VoteMessage} for the proposed block, converted to a {@link JsonNode}.
     */
    private static JsonNode buildVoteMessage(int nodeId, Block block, String signature, String proposerSignature) {
        JsonBlock jsonBlock = block.toJsonBlock();
        VoteMessage voteMessage = new VoteMessage(nodeId, jsonBlock, signature, proposerSignature);
        return objectMapper.valueToTree(voteMessage);
    }

}

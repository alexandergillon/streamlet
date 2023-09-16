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
package com.github.alexandergillon.streamlet.broadcast.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexandergillon.streamlet.broadcast.models.BroadcastMessage;
import com.github.alexandergillon.streamlet.broadcast.models.PayloadMessage;
import com.github.alexandergillon.streamlet.broadcast.services.KafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Implementation of a {@link KafkaService}. */
@Slf4j
@Service
@RequiredArgsConstructor
// If Kafka is enabled when unit testing, context will never come up because application cannot connect to broker
@Profile("!unittests")
public class KafkaServiceImpl implements KafkaService {

    // Constants from Spring properties
    @Value("${streamlet.participants}")
    private int numNodes;
    @Value("${streamlet.kafka.payload-topic.prefix}")
    private String payloadTopicPrefix;
    @Value("${streamlet.kafka.propose-topic.prefix}")
    private String proposeTopicPrefix;
    @Value("${streamlet.kafka.vote-topic.prefix}")
    private String voteTopicPrefix;

    // Autowired dependencies (via RequiredArgsConstructor)
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Instance variables
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @KafkaListener(topics = "${streamlet.kafka.broadcast-topic.name}", properties = {"spring.json.value.default.type=com.github.alexandergillon.streamlet.broadcast.models.BroadcastMessage"})
    public void processBroadcast(BroadcastMessage message) {
        log.info("Received broadcast of type {} from {}: {}", message.getMessageType(), message.getSender(), message.getMessage().toString());
        switch (message.getMessageType()) {
            case "propose" -> broadcastProposal(message.getSender(), message.getMessage());
            case "vote" -> broadcastVote(message.getSender(), message.getMessage());
            default -> {
                log.error("Received broadcast message with unrecognized message type: " + message);
                throw new RuntimeException("Received broadcast message with unrecognized message type: " + message);
            }
        }
    }

    @Override
    public void broadcastPayload(String username, String text) {
        try {
            PayloadMessage message = new PayloadMessage(username, text, System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(message);
            log.info("Broadcasting message from {} with text {} to nodes", username, text);
            for (int i = 0; i < numNodes; i++) {
                kafkaTemplate.send(payloadTopicPrefix + i, json);  // TODO: fault tolerance - check it got to broker
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Broadcasts a proposal to all nodes but the proposer.
     *
     * @param proposer The sender of the proposal.
     * @param proposalJson The proposal message, as a JSON string.
     */
    private void broadcastProposal(int proposer, JsonNode proposalJson) {
        verifyProposal(proposalJson);

        for (int i = 0; i < numNodes; i++) {
            if (i == proposer) continue;
            kafkaTemplate.send(proposeTopicPrefix + i, proposalJson.toString()); // TODO: fault tolerance - check it got to broker
        }
    }

    /**
     * Performs a number of validations on a proposal message. If any of these fail, throws an exception, causing
     * the proposal to not be broadcast and an error logged.
     *
     * @param proposalJson The proposal message, as a JSON string.
     */
    private void verifyProposal(JsonNode proposalJson) {
        if (proposalJson.get("nodeId") == null ||
                proposalJson.get("block") == null ||
                proposalJson.at("/block/parentHash").isMissingNode() ||
                proposalJson.at("/block/epoch").isMissingNode() ||
                proposalJson.at("/block/payload").isMissingNode() ||
                proposalJson.get("signature") == null) {
            throw new IllegalArgumentException("Proposal message is not well-formed (has missing fields):" + proposalJson);
        }

        if (!proposalJson.get("nodeId").isInt() ||
                !proposalJson.get("block").isObject() ||
                !proposalJson.at("/block/parentHash").isTextual() ||
                !proposalJson.at("/block/epoch").isInt() ||
                !proposalJson.at("/block/payload").isTextual() ||
                !proposalJson.get("signature").isTextual()) {
            throw new IllegalArgumentException("Proposal message is not well-formed (has incorrect types):" + proposalJson);
        }
    }

    /**
     * Broadcasts a vote to all nodes but the voter.
     *
     * @param voter The sender of the vote.
     * @param voteJson The vote message, as a JSON string.
     */
    private void broadcastVote(int voter, JsonNode voteJson) {
        verifyVote(voteJson);

        for (int i = 0; i < numNodes; i++) {
            if (i == voter) continue;
            kafkaTemplate.send(voteTopicPrefix + i, voteJson.toString()); // TODO: fault tolerance - check it got to broker
        }
    }

    /**
     * Performs a number of validations on a vote message. If any of these fail, throws an exception, causing
     * the vote to not be broadcast and an error logged.
     *
     * @param voteJson The vote message, as a JSON string.
     */
    private void verifyVote(JsonNode voteJson) {
        if (voteJson.get("nodeId") == null ||
                voteJson.get("block") == null ||
                voteJson.at("/block/parentHash").isMissingNode() ||
                voteJson.at("/block/epoch").isMissingNode() ||
                voteJson.at("/block/payload").isMissingNode() ||
                voteJson.get("signature") == null ||
                voteJson.get("proposerSignature") == null) {
            throw new IllegalArgumentException("Vote message is not well-formed (has missing fields):" + voteJson);
        }

        if (!voteJson.get("nodeId").isInt() ||
                !voteJson.get("block").isObject() ||
                !voteJson.at("/block/parentHash").isTextual() ||
                !voteJson.at("/block/epoch").isInt() ||
                !voteJson.at("/block/payload").isTextual() ||
                !voteJson.get("signature").isTextual() ||
                !voteJson.get("proposerSignature").isTextual()) {
            throw new IllegalArgumentException("Vote message is not well-formed (has incorrect types):" + voteJson);
        }
    }

}

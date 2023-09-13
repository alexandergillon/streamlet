package com.github.alexandergillon.streamlet.broadcast.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexandergillon.streamlet.broadcast.models.BroadcastMessage;
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
        switch (message.getMessageType()) {
            case "propose" -> broadcastProposal(message.getSender(), message.getMessage());
            case "vote" -> broadcastVote(message.getSender(), message.getMessage());
            default -> {
                log.error("Received broadcast message with unrecognized message type: " + message);
                throw new RuntimeException("Received broadcast message with unrecognized message type: " + message);
            }
        }
    }

    /**
     * Broadcasts a proposal to all nodes but the proposer.
     *
     * @param proposer The sender of the proposal.
     * @param proposalJson The proposal message, as a JSON string.
     */
    private void broadcastProposal(int proposer, String proposalJson) {
        verifyProposal(proposalJson);

        for (int i = 0; i < numNodes; i++) {
            if (i == proposer) continue;
            kafkaTemplate.send(proposeTopicPrefix + i, proposalJson); // TODO: fault tolerance - check it got to broker
        }
    }

    /**
     * Performs a number of validations on a proposal message. If any of these fail, throws an exception, causing
     * the proposal to not be broadcast and an error logged.
     *
     * @param proposalJson The proposal message, as a JSON string.
     */
    private void verifyProposal(String proposalJson) {
        try {
            JsonNode json = objectMapper.readTree(proposalJson);

            if (json.get("nodeId") == null ||
                    json.get("block") == null ||
                    json.at("/block/parentHash").isMissingNode() ||
                    json.at("/block/epoch").isMissingNode() ||
                    json.at("/block/payload").isMissingNode() ||
                    json.get("signature") == null) {
                throw new IllegalArgumentException("Proposal message is not well-formed (has missing fields):" + proposalJson);
            }

            if (!json.get("nodeId").isInt() ||
                    !json.get("block").isObject() ||
                    !json.at("/block/parentHash").isTextual() ||
                    !json.at("/block/epoch").isInt() ||
                    !json.at("/block/payload").isTextual() ||
                    !json.get("signature").isTextual()) {
                throw new IllegalArgumentException("Proposal message is not well-formed (has incorrect types):" + proposalJson);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Broadcasts a vote to all nodes but the voter.
     *
     * @param voter The sender of the vote.
     * @param voteJson The vote message, as a JSON string.
     */
    private void broadcastVote(int voter, String voteJson) {
        verifyVote(voteJson);

        for (int i = 0; i < numNodes; i++) {
            if (i == voter) continue;
            kafkaTemplate.send(voteTopicPrefix + i, voteJson); // TODO: fault tolerance - check it got to broker
        }
    }

    /**
     * Performs a number of validations on a vote message. If any of these fail, throws an exception, causing
     * the vote to not be broadcast and an error logged.
     *
     * @param voteJson The vote message, as a JSON string.
     */
    private void verifyVote(String voteJson) {
        try {
            JsonNode json = objectMapper.readTree(voteJson);

            if (json.get("nodeId") == null ||
                    json.get("block") == null ||
                    json.at("/block/parentHash").isMissingNode() ||
                    json.at("/block/epoch").isMissingNode() ||
                    json.at("/block/payload").isMissingNode() ||
                    json.get("signature") == null ||
                    json.get("proposerSignature") == null) {
                throw new IllegalArgumentException("Vote message is not well-formed (has missing fields):" + voteJson);
            }

            if (!json.get("nodeId").isInt() ||
                    !json.get("block").isObject() ||
                    !json.at("/block/parentHash").isTextual() ||
                    !json.at("/block/epoch").isInt() ||
                    !json.at("/block/payload").isTextual() ||
                    !json.get("signature").isTextual() ||
                    !json.get("proposerSignature").isTextual()) {
                throw new IllegalArgumentException("Vote message is not well-formed (has incorrect types):" + voteJson);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}

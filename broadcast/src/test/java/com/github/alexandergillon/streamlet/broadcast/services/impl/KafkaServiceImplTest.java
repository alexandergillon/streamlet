package com.github.alexandergillon.streamlet.broadcast.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexandergillon.streamlet.broadcast.models.BroadcastMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class KafkaServiceImplTest {

    @Value("${streamlet.participants}")
    private int numNodes;
    @Value("${streamlet.kafka.propose-topic.prefix}")
    private String proposeTopicPrefix;
    @Value("${streamlet.kafka.vote-topic.prefix}")
    private String voteTopicPrefix;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaServiceImpl kafkaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void injectProperties() {
        ReflectionTestUtils.setField(kafkaService, "numNodes", numNodes);
        ReflectionTestUtils.setField(kafkaService, "proposeTopicPrefix", proposeTopicPrefix);
        ReflectionTestUtils.setField(kafkaService, "voteTopicPrefix", voteTopicPrefix);
    }

    // Tests that broadcasting proposals works correctly
    @Test
    public void testProposalBroadcast() throws JsonProcessingException {
        int sender = 3;
        String proposalMessage = """
                {
                    "nodeId": 3,
                    "block": {
                        "parentHash": "w5onHinZsVXW/NxyN3XR9Q==",
                        "epoch": 14,
                        "payload": "FSKvFi7CqHwS8Bs6lmPqgQ=="
                    },
                    "signature": "gzqztEJoi5atTxMGG4Ysiw=="
                }
                """;
        JsonNode jsonNode = objectMapper.readTree(proposalMessage);

        HashSet<String> topicsBroadcastTo = new HashSet<>();
        Answer<CompletableFuture<SendResult<String, String>>> answer = invocationOnMock -> {
            topicsBroadcastTo.add(invocationOnMock.getArgument(0));
            JsonNode broadcastJson = objectMapper.readTree((String) invocationOnMock.getArgument(1));
            assertEquals(jsonNode, broadcastJson);
            return CompletableFuture.completedFuture(null);
        };

        when(kafkaTemplate.send(anyString(), anyString())).thenAnswer(answer);
        kafkaService.processBroadcast(new BroadcastMessage(sender, "propose", proposalMessage));

        verify(kafkaTemplate, times(numNodes-1)).send(anyString(), anyString());
        for (int i = 0; i < numNodes; i++) {
            if (i == sender) {
                assertFalse(topicsBroadcastTo.contains(proposeTopicPrefix + i));
            } else {
                assertTrue(topicsBroadcastTo.contains(proposeTopicPrefix + i));
            }
        }
    }

    // Tests that broadcasting proposals works correctly
    @Test
    public void testVoteBroadcast() throws JsonProcessingException {
        int sender = 2;
        String voteMessage = """
                {
                    "nodeId": 2,
                    "block": {
                        "parentHash": "e9NCMC75Np8sK6e/SVVHeA==",
                        "epoch": 7,
                        "payload": "6fC2GOg956f7bCIFo6vvGQ=="
                    },
                    "signature": "7BU5A6NF2QZZSeVrrE/pFg==",
                    "proposerSignature": "UjoJkj2GDoxNcy0ua6Qt9w=="
                }
                """;
        JsonNode jsonNode = objectMapper.readTree(voteMessage);

        HashSet<String> topicsBroadcastTo = new HashSet<>();
        Answer<CompletableFuture<SendResult<String, String>>> answer = invocationOnMock -> {
            topicsBroadcastTo.add(invocationOnMock.getArgument(0));
            JsonNode broadcastJson = objectMapper.readTree((String) invocationOnMock.getArgument(1));
            assertEquals(jsonNode, broadcastJson);
            return CompletableFuture.completedFuture(null);
        };

        when(kafkaTemplate.send(anyString(), anyString())).thenAnswer(answer);
        kafkaService.processBroadcast(new BroadcastMessage(sender, "vote", voteMessage));

        verify(kafkaTemplate, times(numNodes-1)).send(anyString(), anyString());
        for (int i = 0; i < numNodes; i++) {
            if (i == sender) {
                assertFalse(topicsBroadcastTo.contains(voteTopicPrefix + i));
            } else {
                assertTrue(topicsBroadcastTo.contains(voteTopicPrefix + i));
            }
        }
    }


}
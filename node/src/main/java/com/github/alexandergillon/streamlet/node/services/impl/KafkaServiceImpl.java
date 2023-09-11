package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.ProposeMessage;
import com.github.alexandergillon.streamlet.node.models.VoteMessage;
import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import com.github.alexandergillon.streamlet.node.services.KafkaService;
import com.github.alexandergillon.streamlet.node.util.SerializationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Implementation of a {@link KafkaService}. */
@Slf4j
@Service
@RequiredArgsConstructor
// If Kafka is enabled when unit testing, context will never come up because application cannot connect to broker
@Profile("!unittests")
public class KafkaServiceImpl implements KafkaService {

    // Constants from Spring properties
    @Value("${streamlet.node.id}")
    private int nodeId;
    @Value("${streamlet.kafka.broadcast-topic.name}")
    private String broadcastTopicName;

    // Autowired dependencies (via RequiredArgsConstructor)
    private final BlockchainService blockchainService;
    private final CryptographyService cryptographyService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @KafkaListener(topics = "proposalsForNode" + "${streamlet.node.id}", properties = {"spring.json.value.default.type=com.github.alexandergillon.streamlet.node.models.ProposeMessage"})
    public void processProposal(ProposeMessage message) {
        byte[] parentHash = Base64.getDecoder().decode(message.getBlock().getParentHash());
        byte[] payload = Base64.getDecoder().decode(message.getBlock().getPayload());
        Block proposedBlock = new Block(parentHash, message.getBlock().getEpoch(), payload);

        byte[] signature = Base64.getDecoder().decode(message.getSignature());

        if (blockchainService.processProposedBlock(proposedBlock, message.getNodeId(), signature)) {
            String voteBroadcast = SerializationUtils.buildVoteBroadcast(nodeId, proposedBlock,
                    cryptographyService.signBase64(proposedBlock), message.getSignature());
            broadcast(voteBroadcast);
        }
    }

    @Override
    @KafkaListener(topics = "votesForNode" + "${streamlet.node.id}", properties = {"spring.json.value.default.type=com.github.alexandergillon.streamlet.node.models.VoteMessage"})
    public void processVote(VoteMessage message) {
        byte[] parentHash = Base64.getDecoder().decode(message.getBlock().getParentHash());
        byte[] payload = Base64.getDecoder().decode(message.getBlock().getPayload());
        Block block = new Block(parentHash, message.getBlock().getEpoch(), payload);

        byte[] signature = Base64.getDecoder().decode(message.getSignature());
        byte[] proposerSignature = Base64.getDecoder().decode(message.getProposerSignature());

        blockchainService.processBlockVote(block, message.getNodeId(), signature, proposerSignature);
    }

    @Override
    public void broadcast(String message) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(broadcastTopicName, message);
        try {
            // TODO: handle timeout
            future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Exception while waiting for Kafka message to send.", e);
            throw new RuntimeException(e);
        }
    }

}

package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.PayloadMessage;
import com.github.alexandergillon.streamlet.node.models.ProposeMessage;
import com.github.alexandergillon.streamlet.node.models.VoteMessage;
import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import com.github.alexandergillon.streamlet.node.services.KafkaListeningService;
import com.github.alexandergillon.streamlet.node.services.KafkaSendingService;
import com.github.alexandergillon.streamlet.node.services.PayloadService;
import com.github.alexandergillon.streamlet.node.util.SerializationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Base64;

/** Implementation of a {@link KafkaListeningService}. */
@Slf4j
@Service
@RequiredArgsConstructor
// If Kafka is enabled when unit testing, context will never come up because application cannot connect to broker
@Profile("!unittests")
public class KafkaListeningServiceImpl implements KafkaListeningService {

    // Constants from Spring properties
    @Value("${streamlet.node.id}")
    private int nodeId;

    // Autowired dependencies (via RequiredArgsConstructor)
    private final BlockchainService blockchainService;
    private final CryptographyService cryptographyService;
    private final PayloadService payloadService;
    private final KafkaSendingService kafkaSendingService;

    @Override
    @KafkaListener(topics = "payloadsForNode" + "${streamlet.node.id}", properties = {"spring.json.value.default.type=com.github.alexandergillon.streamlet.node.models.PayloadMessage"})
    public void processPayload(PayloadMessage message) {
        log.info("Received proposed payload from user {}: {}", message.getUsername(), message.getText());
        payloadService.addPendingMessage(message);
    }

    @Override
    @KafkaListener(topics = "proposalsForNode" + "${streamlet.node.id}", properties = {"spring.json.value.default.type=com.github.alexandergillon.streamlet.node.models.ProposeMessage"})
    public void processProposal(ProposeMessage message) {
        log.info("Received proposed block from node {}: {}", message.getNodeId(), message.getBlock().toString());
        byte[] parentHash = Base64.getDecoder().decode(message.getBlock().getParentHash());
        byte[] payload = Base64.getDecoder().decode(message.getBlock().getPayload());
        Block proposedBlock = new Block(parentHash, message.getBlock().getEpoch(), payload);

        byte[] signature = Base64.getDecoder().decode(message.getSignature());

        if (blockchainService.processProposedBlock(proposedBlock, message.getNodeId(), signature)) {
            String voteBroadcast = SerializationUtils.buildVoteBroadcast(nodeId, proposedBlock,
                    cryptographyService.signBase64(proposedBlock), message.getSignature());
            kafkaSendingService.broadcast(voteBroadcast);
        }
    }

    @Override
    @KafkaListener(topics = "votesForNode" + "${streamlet.node.id}", properties = {"spring.json.value.default.type=com.github.alexandergillon.streamlet.node.models.VoteMessage"})
    public void processVote(VoteMessage message) {
        log.info("Received vote on block from node {}: {}", message.getNodeId(), message.getBlock().toString());
        byte[] parentHash = Base64.getDecoder().decode(message.getBlock().getParentHash());
        byte[] payload = Base64.getDecoder().decode(message.getBlock().getPayload());
        Block block = new Block(parentHash, message.getBlock().getEpoch(), payload);

        byte[] signature = Base64.getDecoder().decode(message.getSignature());
        byte[] proposerSignature = Base64.getDecoder().decode(message.getProposerSignature());

        blockchainService.processBlockVote(block, message.getNodeId(), signature, proposerSignature);
    }

}

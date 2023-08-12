package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.models.ProposeMessage;
import com.github.alexandergillon.streamlet.node.models.VoteMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/** Service to handle Kafka messages (including both producing and consuming them). */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    @KafkaListener(topics = "proposalsForNode" + "${streamlet.node.id}", properties = {"spring.json.value.default.type=com.github.alexandergillon.streamlet.node.models.ProposeMessage"})
    public void processProposal(ProposeMessage message) {
        log.info(message.toString());
    }

    @KafkaListener(topics = "votesForNode" + "${streamlet.node.id}", properties = {"spring.json.value.default.type=com.github.alexandergillon.streamlet.node.models.VoteMessage"})
    public void processVote(VoteMessage message) {
        log.info(message.toString());
    }

}

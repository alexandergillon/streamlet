package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.services.KafkaSendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Implementation of a {@link KafkaSendingService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSendingServiceImpl implements KafkaSendingService {

    // Constants from Spring properties
    @Value("${streamlet.kafka.broadcast-topic.name}")
    private String broadcastTopicName;

    // Autowired dependencies (via RequiredArgsConstructor)
    private final KafkaTemplate<String, String> kafkaTemplate;

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

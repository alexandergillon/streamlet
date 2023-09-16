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
        log.info("Broadcasting {}", message);
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

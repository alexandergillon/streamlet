package com.github.alexandergillon.streamlet.node.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaSendingServiceImplTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaSendingServiceImpl kafkaSendingService;

    @Mock
    CompletableFuture<SendResult<String, String>> result;

    @BeforeEach
    public void injectProperties() {
        ReflectionTestUtils.setField(kafkaSendingService, "broadcastTopicName", "topicName");
    }

    // Tests that broadcast functions correctly
    @Test
    public void testBroadcast() throws ExecutionException, InterruptedException, TimeoutException {
        String randomMessage = UUID.randomUUID().toString() + UUID.randomUUID().toString() + UUID.randomUUID().toString() + UUID.randomUUID().toString() + UUID.randomUUID().toString();
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(result);
        when(result.get(anyLong(), any(TimeUnit.class))).thenReturn(null);
        kafkaSendingService.broadcast(randomMessage);
        verify(kafkaTemplate).send("topicName", randomMessage);
    }

}
package com.github.alexandergillon.streamlet.broadcast.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
// If Kafka is enabled when unit testing, context will never come up because application cannot connect to broker
@Profile("!unittests")
public class KafkaConfig {

    @Value("${streamlet.kafka.broadcast-topic.name}")
    private String broadcastTopicName;

    @Bean
    public NewTopic broadcastTopic() {
        return TopicBuilder.name(broadcastTopicName).partitions(1).replicas(3).build();
    }

}

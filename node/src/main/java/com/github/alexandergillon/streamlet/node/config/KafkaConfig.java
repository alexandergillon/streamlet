package com.github.alexandergillon.streamlet.node.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("proposalsForNode" + "${streamlet.node.id}")
    private String proposalTopicName;

    @Value("votesForNode" + "${streamlet.node.id}")
    private String voteTopicName;

    @Bean
    public NewTopic proposalTopic() {
        return TopicBuilder.name(proposalTopicName).partitions(1).replicas(3).build();
    }

    @Bean
    public NewTopic voteTopic() {
        return TopicBuilder.name(voteTopicName).partitions(1).replicas(3).build();
    }

}

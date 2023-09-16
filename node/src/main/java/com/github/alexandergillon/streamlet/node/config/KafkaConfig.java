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
package com.github.alexandergillon.streamlet.node.config;

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

    @Value("payloadsForNode" + "${streamlet.node.id}")
    private String payloadTopicName;

    @Value("proposalsForNode" + "${streamlet.node.id}")
    private String proposalTopicName;

    @Value("votesForNode" + "${streamlet.node.id}")
    private String voteTopicName;

    @Bean
    public NewTopic payloadTopic() {
        return TopicBuilder.name(payloadTopicName).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic proposalTopic() {
        return TopicBuilder.name(proposalTopicName).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic voteTopic() {
        return TopicBuilder.name(voteTopicName).partitions(1).replicas(1).build();
    }

}

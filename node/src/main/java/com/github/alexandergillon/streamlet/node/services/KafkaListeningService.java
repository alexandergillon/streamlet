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
package com.github.alexandergillon.streamlet.node.services;

import com.github.alexandergillon.streamlet.node.models.PayloadMessage;
import com.github.alexandergillon.streamlet.node.models.ProposeMessage;
import com.github.alexandergillon.streamlet.node.models.VoteMessage;

/** Service to handle listening to Kafka topics. */
public interface KafkaListeningService {

    /**
     * Processes a payload message from Kafka. This method should be annotated with {@code @KafkaListener}
     * and appropriate annotation parameters to pick up the correct messages from Kafka.
     *
     * @param message The payload message from Kafka, to be processed.
     */
    void processPayload(PayloadMessage message);

    /**
     * Processes a proposal message from Kafka. This method should be annotated with {@code @KafkaListener}
     * and appropriate annotation parameters to pick up the correct messages from Kafka.
     *
     * @param message The proposal message from Kafka, to be processed.
     */
    void processProposal(ProposeMessage message);

    /**
     * Processes a vote message from Kafka. This method should be annotated with {@code @KafkaListener}
     * and appropriate annotation parameters to pick up the correct messages from Kafka.
     *
     * @param message The vote message from Kafka, to be processed.
     */
    void processVote(VoteMessage message);

}

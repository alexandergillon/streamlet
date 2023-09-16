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
package com.github.alexandergillon.streamlet.broadcast.controllers;

import com.github.alexandergillon.streamlet.broadcast.models.PayloadRequest;
import com.github.alexandergillon.streamlet.broadcast.services.KafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Controller to allow users to propose messages to be included in the blockchain. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PayloadController {

    // Autowired dependencies via RequiredArgsConstructor
    private final KafkaService kafkaService;

    /**
     * Endpoint to allow users to submit messages to be included in the blockchain.
     *
     * @param request Details about the message to be submitted. Username must not contain a colon character.
     * @return A small message on success. Status codes: 200 OK if the message was sent to nodes, or 400 Bad Request if
     * the request is missing parameters.
     */
    @PostMapping("/send")
    public String processMessage(@RequestBody PayloadRequest request) {
        if (request.getUsername() == null || request.getText() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (request.getUsername().contains(":")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot contain ':'.");

        kafkaService.broadcastPayload(request.getUsername(), request.getText());
        return "Message submitted for inclusion in the blockchain.";
    }

}

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

import com.github.alexandergillon.streamlet.broadcast.services.KafkaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PayloadControllerTest {

    @MockBean
    private KafkaService kafkaService;

    @Autowired
    private MockMvc mockMvc;

    // Tests that a payload broadcast functions correctly
    @Test
    public void testPayloadBroadcast() throws Exception {
        String username = "d1ad26fb-6a44-4a82-8b55-a37183e754a0";
        String text = "b8d45ebf-43d9-4c9f-a5ac-cb512a7e88c5";

        String json = """
                {
                    "username": "d1ad26fb-6a44-4a82-8b55-a37183e754a0",
                    "text": "b8d45ebf-43d9-4c9f-a5ac-cb512a7e88c5"
                }
                """;

        mockMvc.perform(post("/send").content(json).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
        verify(kafkaService).broadcastPayload(username, text);
    }

    // Tests that bad payloads are rejected
    @Test
    public void testBadPayload() throws Exception {
        String noText = """
                {
                    "username": "d1ad26fb-6a44-4a82-8b55-a37183e754a0",
                }
                """;

        String noUsername = """
                {
                    "text": "b8d45ebf-43d9-4c9f-a5ac-cb512a7e88c5"
                }
                """;

        String noContent = """
                {
                    
                }
                """;

        String illegalUsername = """
                {
                    "username": "this:username:is:illegal",
                    "text": "b8d45ebf-43d9-4c9f-a5ac-cb512a7e88c5"
                }
                """;

        mockMvc.perform(post("/send").content(noText).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        mockMvc.perform(post("/send").content(noUsername).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        mockMvc.perform(post("/send").content(noContent).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        mockMvc.perform(post("/send").content(illegalUsername).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

}
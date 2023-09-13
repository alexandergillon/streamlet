package com.github.alexandergillon.streamlet.broadcast.controllers;

import com.github.alexandergillon.streamlet.broadcast.services.KafkaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
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
package com.github.alexandergillon.streamlet.node.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.PayloadMessage;
import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BlockchainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlockchainService blockchainService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testGetChainReadable() throws Exception {
        Block block0 = TestUtils.getRandomReadableBlock();
        Block block1 = TestUtils.getRandomReadableBlock();
        Block block2 = TestUtils.getRandomReadableBlock();
        Block block3 = TestUtils.getRandomReadableBlock();
        Block block4 = TestUtils.getRandomReadableBlock();

        when(blockchainService.getFinalizedChain()).thenReturn(List.of(block0, block1, block2, block3, block4));
        MvcResult mvcResult = mockMvc.perform(get("/chain/readable"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andReturn();

        String text = mvcResult.getResponse().getContentAsString();
        List<String> lines = text.lines().toList();
        assertEquals(PayloadMessage.fromStringBytes(block0.getPayload()).toString(), lines.get(0));
        assertEquals(PayloadMessage.fromStringBytes(block1.getPayload()).toString(), lines.get(1));
        assertEquals(PayloadMessage.fromStringBytes(block2.getPayload()).toString(), lines.get(2));
        assertEquals(PayloadMessage.fromStringBytes(block3.getPayload()).toString(), lines.get(3));
        assertEquals(PayloadMessage.fromStringBytes(block4.getPayload()).toString(), lines.get(4));
    }

    // Tests that getting the chain works correctly
    @Test
    public void testGetChainJson() throws Exception {
        Block block0 = TestUtils.getRandomReadableBlock();
        Block block1 = TestUtils.getRandomReadableBlock();
        Block block2 = TestUtils.getRandomReadableBlock();
        Block block3 = TestUtils.getRandomReadableBlock();
        Block block4 = TestUtils.getRandomReadableBlock();

        when(blockchainService.getFinalizedChain()).thenReturn(List.of(block0, block1, block2, block3, block4));
        MvcResult mvcResult = mockMvc.perform(get("/chain/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(body);
        assertTrue(jsonNode.isArray());

        PayloadMessage jsonMessage0 = objectMapper.treeToValue(jsonNode.get(0), PayloadMessage.class);
        PayloadMessage jsonMessage1 = objectMapper.treeToValue(jsonNode.get(1), PayloadMessage.class);
        PayloadMessage jsonMessage2 = objectMapper.treeToValue(jsonNode.get(2), PayloadMessage.class);
        PayloadMessage jsonMessage3 = objectMapper.treeToValue(jsonNode.get(3), PayloadMessage.class);
        PayloadMessage jsonMessage4 = objectMapper.treeToValue(jsonNode.get(4), PayloadMessage.class);

        assertEquals(jsonMessage0, PayloadMessage.fromStringBytes(block0.getPayload()));
        assertEquals(jsonMessage1, PayloadMessage.fromStringBytes(block1.getPayload()));
        assertEquals(jsonMessage2, PayloadMessage.fromStringBytes(block2.getPayload()));
        assertEquals(jsonMessage3, PayloadMessage.fromStringBytes(block3.getPayload()));
        assertEquals(jsonMessage4, PayloadMessage.fromStringBytes(block4.getPayload()));
    }

}
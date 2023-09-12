package com.github.alexandergillon.streamlet.node.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.JsonBlock;
import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    // Tests that getting the chain works correctly
    @Test
    public void testGetChain() throws Exception {
        Block block0 = TestUtils.getRandomBlock();
        Block block1 = TestUtils.getRandomBlock();
        Block block2 = TestUtils.getRandomBlock();
        Block block3 = TestUtils.getRandomBlock();
        Block block4 = TestUtils.getRandomBlock();

        when(blockchainService.getFinalizedChain()).thenReturn(List.of(block0, block1, block2, block3, block4));
        MvcResult mvcResult = mockMvc.perform(get("/chain/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(body);
        assertTrue(jsonNode.isArray());

        JsonBlock jsonBlock0 = objectMapper.treeToValue(jsonNode.get(0), JsonBlock.class);
        JsonBlock jsonBlock1 = objectMapper.treeToValue(jsonNode.get(1), JsonBlock.class);
        JsonBlock jsonBlock2 = objectMapper.treeToValue(jsonNode.get(2), JsonBlock.class);
        JsonBlock jsonBlock3 = objectMapper.treeToValue(jsonNode.get(3), JsonBlock.class);
        JsonBlock jsonBlock4 = objectMapper.treeToValue(jsonNode.get(4), JsonBlock.class);

        assertEquals(jsonBlock0, block0.toJsonBlock());
        assertEquals(jsonBlock1, block1.toJsonBlock());
        assertEquals(jsonBlock2, block2.toJsonBlock());
        assertEquals(jsonBlock3, block3.toJsonBlock());
        assertEquals(jsonBlock4, block4.toJsonBlock());
    }

}
package com.github.alexandergillon.streamlet.node.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.ProposeMessage;
import com.github.alexandergillon.streamlet.node.models.VoteMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerializationUtilsTest {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testIntTo4Bytes() {
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00}, SerializationUtils.intToFourBytesBigEndian(0));
        assertArrayEquals(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}, SerializationUtils.intToFourBytesBigEndian(-1));
        assertArrayEquals(new byte[]{(byte)0x13, (byte)0x99, (byte)0x33, (byte)0xD6}, SerializationUtils.intToFourBytesBigEndian(328807382));
        assertArrayEquals(new byte[]{(byte)0x00, (byte)0x0E, (byte)0x3B, (byte)0xCC}, SerializationUtils.intToFourBytesBigEndian(932812));
    }

    @Test
    public void test4BytesToInt() {
        assertEquals(0, SerializationUtils.fourBytesToIntBigEndian(new byte[]{(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00}));
        assertEquals(-1, SerializationUtils.fourBytesToIntBigEndian(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}));
        assertEquals(328807382, SerializationUtils.fourBytesToIntBigEndian(new byte[]{(byte)0x13, (byte)0x99, (byte)0x33, (byte)0xD6}));
        assertEquals(932812, SerializationUtils.fourBytesToIntBigEndian(new byte[]{(byte)0x00, (byte)0x0E, (byte)0x3B, (byte)0xCC}));
    }

    @Test
    public void test4BytesToIntException() {
        assertThrows(IllegalArgumentException.class, () -> SerializationUtils.fourBytesToIntBigEndian(new byte[]{(byte)0x23, (byte)0x2B}));
        assertThrows(IllegalArgumentException.class, () -> SerializationUtils.fourBytesToIntBigEndian(new byte[]{(byte)0xA7, (byte)0xDE, (byte)0x13, (byte)0x89, (byte)0xC6}));
    }

    @Test
    public void testBuildProposeBroadcast() throws JsonProcessingException {
        int nodeId = 7;
        Block block = TestUtils.getRandomBlock();
        String signature = "2I2MJQCYn2atWcNj/VHJXg==";  // not a legitimate signature, but shouldn't matter for test

        String jsonString = SerializationUtils.buildProposeBroadcast(nodeId, block, signature);
        JsonNode json = objectMapper.readTree(jsonString);

        assertEquals(nodeId, json.get("sender").intValue());
        assertEquals("propose", json.get("messageType").textValue());

        ProposeMessage message = objectMapper.treeToValue(json.get("message"), ProposeMessage.class);
        assertEquals(nodeId, message.getNodeId());
        assertEquals(block.getParentHashBase64(), message.getBlock().getParentHash());
        assertEquals(block.getEpoch(), message.getBlock().getEpoch());
        assertEquals(block.getPayloadBase64(), message.getBlock().getPayload());
        assertEquals(signature, message.getSignature());
    }

    @Test
    public void testBuildVoteBroadcast() throws JsonProcessingException {
        int nodeId = 26;
        Block block = TestUtils.getRandomBlock();
        String signature = "WkDzaXFTnBVn73ff70tXyg=="; // not a legitimate signature, but shouldn't matter for test
        String proposerSignature = "KXKUryE52JF0/Lu45QUN7w=="; // not a legitimate signature, but shouldn't matter for test

        String jsonString = SerializationUtils.buildVoteBroadcast(nodeId, block, signature, proposerSignature);
        JsonNode json = objectMapper.readTree(jsonString);

        assertEquals(nodeId, json.get("sender").intValue());
        assertEquals("vote", json.get("messageType").textValue());

        VoteMessage message = objectMapper.treeToValue(json.get("message"), VoteMessage.class);
        assertEquals(nodeId, message.getNodeId());
        assertEquals(block.getParentHashBase64(), message.getBlock().getParentHash());
        assertEquals(block.getEpoch(), message.getBlock().getEpoch());
        assertEquals(block.getPayloadBase64(), message.getBlock().getPayload());
        assertEquals(signature, message.getSignature());
        assertEquals(proposerSignature, message.getProposerSignature());
    }

}
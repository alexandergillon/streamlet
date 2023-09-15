package com.github.alexandergillon.streamlet.node.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.PayloadMessage;
import com.github.alexandergillon.streamlet.node.models.ProposeMessage;
import com.github.alexandergillon.streamlet.node.models.VoteMessage;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;

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

    // Tests that block lists are correctly converted to text
    @Test
    public void testBlockListToReadableText() throws ParseException {
        Block block0 = TestUtils.getRandomReadableBlock();
        Block block1 = TestUtils.getRandomReadableBlock();
        Block block2 = TestUtils.getRandomReadableBlock();
        Block block3 = TestUtils.getRandomReadableBlock();
        Block block4 = TestUtils.getRandomReadableBlock();

        String text = SerializationUtils.blockListMessagesToReadableText(List.of(block0, block1, block2, block3, block4));
        List<String> lines = text.lines().toList();
        assertEquals(PayloadMessage.fromStringBytes(block0.getPayload()).toString(), lines.get(0));
        assertEquals(PayloadMessage.fromStringBytes(block1.getPayload()).toString(), lines.get(1));
        assertEquals(PayloadMessage.fromStringBytes(block2.getPayload()).toString(), lines.get(2));
        assertEquals(PayloadMessage.fromStringBytes(block3.getPayload()).toString(), lines.get(3));
        assertEquals(PayloadMessage.fromStringBytes(block4.getPayload()).toString(), lines.get(4));
    }

    // Tests that block lists are correctly converted to JSON
    @Test
    public void testBlockListToJson() throws JsonProcessingException, ParseException {
        Block block0 = TestUtils.getRandomReadableBlock();
        Block block1 = TestUtils.getRandomReadableBlock();
        Block block2 = TestUtils.getRandomReadableBlock();
        Block block3 = TestUtils.getRandomReadableBlock();
        Block block4 = TestUtils.getRandomReadableBlock();

        String json = SerializationUtils.blockListMessagesToJson(List.of(block0, block1, block2, block3, block4));

        JsonNode jsonNode = objectMapper.readTree(json);
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
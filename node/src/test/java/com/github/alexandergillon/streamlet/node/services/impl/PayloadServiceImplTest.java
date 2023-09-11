package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.Message;
import com.github.alexandergillon.streamlet.node.services.PayloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PayloadServiceImplTest {

    private PayloadService payloadService;

    @BeforeEach
    public void setupPayloadService() {
        payloadService = new PayloadServiceImpl();
    }

    // Tests that messages are returned in the right order, and are cleared from the buffer when finalized
    @Test
    public void testPayloadOrderImmediateFinalization() {
        assertNull(payloadService.getNextPayload(new HashSet<>()));
        
        Message message1 = TestUtils.randomMessage();
        Message message2 = TestUtils.randomMessage();
        Message message3 = TestUtils.randomMessage();
        Message message4 = TestUtils.randomMessage();
        Message message5 = TestUtils.randomMessage();
        payloadService.addPendingMessage(message1);
        payloadService.addPendingMessage(message2);
        payloadService.addPendingMessage(message3);
        payloadService.addPendingMessage(message4);
        payloadService.addPendingMessage(message5);
        
        assertArrayEquals(message1.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message1.toStringBytes());
        assertArrayEquals(message2.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message2.toStringBytes());
        assertArrayEquals(message3.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message3.toStringBytes());
        assertArrayEquals(message4.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message4.toStringBytes());
        assertArrayEquals(message5.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message5.toStringBytes());
        assertNull(payloadService.getNextPayload(new HashSet<>()));
    }

    // Tests that the correct messages are returned when some pending messages are included in unfinalized chains
    @Test
    public void testPayloadOrderWithUnfinalizedChains() {
        Message message1 = TestUtils.randomMessage();
        Message message2 = TestUtils.randomMessage();
        Message message3 = TestUtils.randomMessage();
        Message message4 = TestUtils.randomMessage();
        Message message5 = TestUtils.randomMessage();
        payloadService.addPendingMessage(message1);
        payloadService.addPendingMessage(message2);
        payloadService.addPendingMessage(message3);
        payloadService.addPendingMessage(message4);
        payloadService.addPendingMessage(message5);
        
        Block blockWithMessage1 = TestUtils.getRandomBlockWithPayload(message1.toStringBytes());
        Block blockWithMessage2 = TestUtils.getRandomBlockWithPayload(message2.toStringBytes());
        Block blockWithMessage3 = TestUtils.getRandomBlockWithPayload(message3.toStringBytes());
        Block blockWithMessage4 = TestUtils.getRandomBlockWithPayload(message4.toStringBytes());
        Block blockWithMessage5 = TestUtils.getRandomBlockWithPayload(message5.toStringBytes());

        assertArrayEquals(message1.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        assertArrayEquals(message2.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1)));
        assertArrayEquals(message3.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage2)));
        assertArrayEquals(message4.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage2, blockWithMessage3)));
        assertArrayEquals(message5.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage2, blockWithMessage3, blockWithMessage4)));
        assertNull(payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage2, blockWithMessage3, blockWithMessage4, blockWithMessage5)));

        assertArrayEquals(message1.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage2, blockWithMessage3)));

        payloadService.finalizedPayload(message2.toStringBytes());

        assertArrayEquals(message1.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        assertArrayEquals(message3.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1)));
        assertArrayEquals(message4.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage3)));
        assertArrayEquals(message5.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage3, blockWithMessage4)));
        assertNull(payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage3, blockWithMessage4, blockWithMessage5)));
    }

    // Tests that finalization of messages not known to the payload service do not cause problems
    @Test
    public void testUnknownFinalizations() {
        Message message1 = TestUtils.randomMessage();
        Message message2 = TestUtils.randomMessage();
        Message message3 = TestUtils.randomMessage();
        Message message4 = TestUtils.randomMessage();
        Message message5 = TestUtils.randomMessage();
        payloadService.addPendingMessage(message1);
        payloadService.addPendingMessage(message2);
        payloadService.addPendingMessage(message3);
        payloadService.addPendingMessage(message4);
        payloadService.addPendingMessage(message5);

        Message unknownMessage1 = TestUtils.randomMessage();
        Message unknownMessage2 = TestUtils.randomMessage();
        Message unknownMessage3 = TestUtils.randomMessage();

        assertArrayEquals(message1.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message1.toStringBytes());
        payloadService.finalizedPayload(unknownMessage1.toStringBytes());
        assertArrayEquals(message2.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message2.toStringBytes());
        payloadService.finalizedPayload(unknownMessage2.toStringBytes());
        assertArrayEquals(message3.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message3.toStringBytes());
        assertArrayEquals(message4.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message4.toStringBytes());
        payloadService.finalizedPayload(unknownMessage3.toStringBytes());
        assertArrayEquals(message5.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        payloadService.finalizedPayload(message5.toStringBytes());
        assertNull(payloadService.getNextPayload(new HashSet<>()));
    }

    // Tests that repeatedly adding the same pending messages does not alter order or duplicate messages
    @Test
    public void testDuplicateMessages() {
        Message message1 = TestUtils.randomMessage();
        Message message2 = TestUtils.randomMessage();
        Message message3 = TestUtils.randomMessage();
        Message message4 = TestUtils.randomMessage();
        Message message5 = TestUtils.randomMessage();
        payloadService.addPendingMessage(message1);
        payloadService.addPendingMessage(message1);
        payloadService.addPendingMessage(message2);
        payloadService.addPendingMessage(message3);
        payloadService.addPendingMessage(message1);
        payloadService.addPendingMessage(message3);
        payloadService.addPendingMessage(message4);
        payloadService.addPendingMessage(message2);
        payloadService.addPendingMessage(message5);

        Block blockWithMessage1 = TestUtils.getRandomBlockWithPayload(message1.toStringBytes());
        Block blockWithMessage2 = TestUtils.getRandomBlockWithPayload(message2.toStringBytes());
        Block blockWithMessage3 = TestUtils.getRandomBlockWithPayload(message3.toStringBytes());
        Block blockWithMessage4 = TestUtils.getRandomBlockWithPayload(message4.toStringBytes());
        Block blockWithMessage5 = TestUtils.getRandomBlockWithPayload(message5.toStringBytes());

        assertArrayEquals(message1.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        assertArrayEquals(message2.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1)));
        assertArrayEquals(message3.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage2)));
        assertArrayEquals(message4.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage2, blockWithMessage3)));
        assertArrayEquals(message5.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage2, blockWithMessage3, blockWithMessage4)));
        assertNull(payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage2, blockWithMessage3, blockWithMessage4, blockWithMessage5)));

        assertArrayEquals(message1.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage2, blockWithMessage3)));

        payloadService.finalizedPayload(message2.toStringBytes());

        assertArrayEquals(message1.toStringBytes(), payloadService.getNextPayload(new HashSet<>()));
        assertArrayEquals(message3.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1)));
        assertArrayEquals(message4.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage3)));
        assertArrayEquals(message5.toStringBytes(), payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage3, blockWithMessage4)));
        assertNull(payloadService.getNextPayload(Set.of(blockWithMessage1, blockWithMessage3, blockWithMessage4, blockWithMessage5)));
    }

}
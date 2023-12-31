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
package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.TestUtils;
import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.PayloadMessage;
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
        
        PayloadMessage message1 = TestUtils.randomMessage();
        PayloadMessage message2 = TestUtils.randomMessage();
        PayloadMessage message3 = TestUtils.randomMessage();
        PayloadMessage message4 = TestUtils.randomMessage();
        PayloadMessage message5 = TestUtils.randomMessage();
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
        PayloadMessage message1 = TestUtils.randomMessage();
        PayloadMessage message2 = TestUtils.randomMessage();
        PayloadMessage message3 = TestUtils.randomMessage();
        PayloadMessage message4 = TestUtils.randomMessage();
        PayloadMessage message5 = TestUtils.randomMessage();
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
        PayloadMessage message1 = TestUtils.randomMessage();
        PayloadMessage message2 = TestUtils.randomMessage();
        PayloadMessage message3 = TestUtils.randomMessage();
        PayloadMessage message4 = TestUtils.randomMessage();
        PayloadMessage message5 = TestUtils.randomMessage();
        payloadService.addPendingMessage(message1);
        payloadService.addPendingMessage(message2);
        payloadService.addPendingMessage(message3);
        payloadService.addPendingMessage(message4);
        payloadService.addPendingMessage(message5);

        PayloadMessage unknownMessage1 = TestUtils.randomMessage();
        PayloadMessage unknownMessage2 = TestUtils.randomMessage();
        PayloadMessage unknownMessage3 = TestUtils.randomMessage();

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
        PayloadMessage message1 = TestUtils.randomMessage();
        PayloadMessage message2 = TestUtils.randomMessage();
        PayloadMessage message3 = TestUtils.randomMessage();
        PayloadMessage message4 = TestUtils.randomMessage();
        PayloadMessage message5 = TestUtils.randomMessage();
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
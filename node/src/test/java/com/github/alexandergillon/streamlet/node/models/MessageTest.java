package com.github.alexandergillon.streamlet.node.models;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    // Tests that colons are not allowed in usernames
    @Test
    public void testNoColonInUsername() {
        assertThrows(IllegalArgumentException.class, () -> new Message("illegal:username", "message", 0));
        assertThrows(IllegalArgumentException.class, () -> new Message().setUsername("illegal:username"));
    }

    // Tests truncation of timestamps to minute-level accuracy
    @RepeatedTest(50)
    public void testTimestampTruncation() {
        String username = UUID.randomUUID().toString();
        String messageText = UUID.randomUUID().toString();
        long nowMillis = truncateToMinutes(System.currentTimeMillis());

        Message message1 = new Message(username, messageText, nowMillis);
        assertEquals(nowMillis, message1.getTimestamp());
        Message message2 = new Message(username, messageText, nowMillis+1);
        assertEquals(nowMillis, message2.getTimestamp());
        Message message3 = new Message(username, messageText, nowMillis+100);
        assertEquals(nowMillis, message3.getTimestamp());
        Message message4 = new Message(username, messageText, nowMillis + ThreadLocalRandom.current().nextInt(200, 50000));
        assertEquals(nowMillis, message4.getTimestamp());

        assertEquals(message1, message2);
        assertEquals(message2, message3);
        assertEquals(message3, message4);
    }

    // Tests truncation of timestamps to minute-level accuracy when setter is used
    @RepeatedTest(50)
    public void testTimestampTruncationWithSetter() {
        String username = UUID.randomUUID().toString();
        String messageText = UUID.randomUUID().toString();
        long nowMillis = truncateToMinutes(System.currentTimeMillis());

        Message message1 = new Message();
        message1.setUsername(username);
        message1.setText(messageText);
        message1.setTimestamp(nowMillis);
        assertEquals(nowMillis, message1.getTimestamp());
        Message message2 = new Message();
        message2.setUsername(username);
        message2.setText(messageText);
        message2.setTimestamp(nowMillis+1);
        assertEquals(nowMillis, message2.getTimestamp());
        Message message3 = new Message();
        message3.setUsername(username);
        message3.setText(messageText);
        message3.setTimestamp(nowMillis+100);
        assertEquals(nowMillis, message3.getTimestamp());
        Message message4 = new Message();
        message4.setUsername(username);
        message4.setText(messageText);
        message4.setTimestamp(nowMillis + ThreadLocalRandom.current().nextInt(200, 50000));
        assertEquals(nowMillis, message4.getTimestamp());

        assertEquals(message1, message2);
        assertEquals(message2, message3);
        assertEquals(message3, message4);
    }

    // Tests that exception is thrown if message is converted to string before being correctly initialized
    @Test
    public void testToStringException() {
        Message message1 = new Message();
        assertThrows(IllegalStateException.class, () -> message1.toString());
        assertThrows(IllegalStateException.class, () -> message1.toStringBytes());

        Message message2 = new Message();
        message2.setUsername("username");
        message2.setText("text");
        assertThrows(IllegalStateException.class, () -> message2.toString());
        assertThrows(IllegalStateException.class, () -> message2.toStringBytes());

        Message message3 = new Message();
        message3.setUsername("username");
        message3.setTimestamp(500000);
        assertThrows(IllegalStateException.class, () -> message3.toString());
        assertThrows(IllegalStateException.class, () -> message3.toStringBytes());

        Message message4 = new Message();
        message4.setText("text");
        message4.setTimestamp(99999);
        assertThrows(IllegalStateException.class, () -> message4.toString());
        assertThrows(IllegalStateException.class, () -> message4.toStringBytes());
    }

    // Tests that messages are correctly converted to a string
    @RepeatedTest(50)
    public void testToString() {
        long timestamp = dateStringToMillisRandomOffsetUTC("2013/03/17 18:34");
        Message message = new Message("sdnjajhu3yuy3", "msesgafdhbdevye", timestamp);

        assertEquals(message.toString(), "2013/03/17 18:34 | sdnjajhu3yuy3: msesgafdhbdevye");
    }

    // Tests that messages can be correctly parsed from a string
    @RepeatedTest(50)
    public void testFromString() throws ParseException {
        Message message = Message.fromString("2017/05/23 15:19 | sadubeuvvfe: psadoeine");
        assertEquals(message.getUsername(), "sadubeuvvfe");
        assertEquals(message.getText(), "psadoeine");
        assertEquals(message.getTimestamp(), dateStringToMillisUTC("2017/05/23 15:19"));
    }

    // Tests that messages are correctly converted to string bytes
    @RepeatedTest(50)
    public void testToStringBytes() {
        long timestamp = dateStringToMillisRandomOffsetUTC("2013/03/17 18:34");
        Message message = new Message("sdnjajhu3yuy3", "msesgafdhbdevye", timestamp);

        assertArrayEquals(message.toStringBytes(), "2013/03/17 18:34 | sdnjajhu3yuy3: msesgafdhbdevye".getBytes(StandardCharsets.US_ASCII));
    }

    // Tests that messages can be correctly parsed from string bytes
    @RepeatedTest(50)
    public void testFromStringBytes() throws ParseException {
        Message message = Message.fromStringBytes("2017/05/23 15:19 | sadubeuvvfe: psadoeine".getBytes(StandardCharsets.US_ASCII));
        assertEquals(message.getUsername(), "sadubeuvvfe");
        assertEquals(message.getText(), "psadoeine");
        assertEquals(message.getTimestamp(), dateStringToMillisUTC("2017/05/23 15:19"));
    }

    // Tests that converting to a string and back again gives the same message
    @RepeatedTest(50)
    public void testToStringFromString() throws ParseException {
        String username = UUID.randomUUID().toString();
        String messageText = UUID.randomUUID().toString();
        long timestamp = dateStringToMillisRandomOffsetUTC("1987/06/22 04:21");

        Message message = new Message(username, messageText, timestamp);
        assertEquals(message, Message.fromString(message.toString()));
    }

    // Tests that converting to string bytes and back again gives the same message
    @RepeatedTest(50)
    public void testToStringFromStringBytes() throws ParseException {
        String username = UUID.randomUUID().toString();
        String messageText = UUID.randomUUID().toString();
        long timestamp = dateStringToMillisRandomOffsetUTC("1987/06/22 04:21");

        Message message = new Message(username, messageText, timestamp);
        assertEquals(message, Message.fromStringBytes(message.toStringBytes()));
    }

    // Truncates a time to minute-level accuracy
    private long truncateToMinutes(long millis) {
        return Instant.ofEpochMilli(millis).truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
    }

    // Parses a date string yyyy/MM/dd HH:mm to a UTC timestamp
    private long dateStringToMillisUTC(String date) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            return simpleDateFormat.parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // Parses a date string yyyy/MM/dd HH:mm to a UTC timestamp, with a random positive offset of up to 20 seconds
    private long dateStringToMillisRandomOffsetUTC(String date) {
        return dateStringToMillisUTC(date) + ThreadLocalRandom.current().nextInt(100, 20000);
    }

}
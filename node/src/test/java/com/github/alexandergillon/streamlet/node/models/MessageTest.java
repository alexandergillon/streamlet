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

    @RepeatedTest(50)
    public void testToString() {
        long timestamp = dateStringToMillisRandomOffsetUTC("2013/03/17 18:34");
        Message message = new Message("sdnjajhu3yuy3", "msesgafdhbdevye", timestamp);

        assertEquals(message.toString(), "2013/03/17 18:34 | sdnjajhu3yuy3: msesgafdhbdevye");
    }

    @RepeatedTest(50)
    public void testFromString() throws ParseException {
        Message message = Message.fromString("2017/05/23 15:19 | sadubeuvvfe: psadoeine");
        assertEquals(message.getUsername(), "sadubeuvvfe");
        assertEquals(message.getText(), "psadoeine");
        assertEquals(message.getTimestamp(), dateStringToMillisUTC("2017/05/23 15:19"));
    }

    @RepeatedTest(50)
    public void testToStringBytes() {
        long timestamp = dateStringToMillisRandomOffsetUTC("2013/03/17 18:34");
        Message message = new Message("sdnjajhu3yuy3", "msesgafdhbdevye", timestamp);

        assertArrayEquals(message.toStringBytes(), "2013/03/17 18:34 | sdnjajhu3yuy3: msesgafdhbdevye".getBytes(StandardCharsets.US_ASCII));
    }

    @RepeatedTest(50)
    public void testFromStringBytes() throws ParseException {
        Message message = Message.fromStringBytes("2017/05/23 15:19 | sadubeuvvfe: psadoeine".getBytes(StandardCharsets.US_ASCII));
        assertEquals(message.getUsername(), "sadubeuvvfe");
        assertEquals(message.getText(), "psadoeine");
        assertEquals(message.getTimestamp(), dateStringToMillisUTC("2017/05/23 15:19"));
    }

    @RepeatedTest(50)
    public void testToStringFromString() throws ParseException {
        String username = UUID.randomUUID().toString();
        String messageText = UUID.randomUUID().toString();
        long timestamp = dateStringToMillisRandomOffsetUTC("1987/06/22 04:21");

        Message message = new Message(username, messageText, timestamp);
        assertEquals(message, Message.fromString(message.toString()));
    }

    @RepeatedTest(50)
    public void testToStringFromStringBytes() throws ParseException {
        String username = UUID.randomUUID().toString();
        String messageText = UUID.randomUUID().toString();
        long timestamp = dateStringToMillisRandomOffsetUTC("1987/06/22 04:21");

        Message message = new Message(username, messageText, timestamp);
        assertEquals(message, Message.fromStringBytes(message.toStringBytes()));
    }


    private long truncateToMinutes(long millis) {
        return Instant.ofEpochMilli(millis).truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
    }

    private long dateStringToMillisUTC(String date) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            return simpleDateFormat.parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private long dateStringToMillisRandomOffsetUTC(String date) {
        return dateStringToMillisUTC(date) + ThreadLocalRandom.current().nextInt(100, 800);
    }

}
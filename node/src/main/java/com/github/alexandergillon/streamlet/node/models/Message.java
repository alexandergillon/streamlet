package com.github.alexandergillon.streamlet.node.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.TimeZone;

/** POJO to represent a message, sent by users to be included in the blockchain. */
@Getter
@EqualsAndHashCode
public class Message {

    /** The username of the user who sent this message. */
    private final String username;

    /** Message text. */
    private final String text;

    /** Timestamp of the message. Used for de-duplication. All timestamps are truncated to minute-level accuracy. */
    private final long timestamp;

    /**
     * Constructor. NOTE: all timestamps are truncated to minute-level accuracy.
     * @throws IllegalArgumentException If username contains a colon character.
     * */
    public Message(String username, String text, long timestamp) throws IllegalArgumentException {
        if (username.contains(":")) throw new IllegalArgumentException("Username " + username + " cannot contain ':'");

        this.username = username;
        this.text = text;
        // truncates timestamp to minutes
        this.timestamp = Instant.ofEpochMilli(timestamp).truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
    }

    @Override
    public String toString() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.format(new Date(timestamp)) + " | " + username + ": " + text;
    }

    /** Converts this message to its string representation, then converts that string to ASCII bytes. */
    public byte[] toStringBytes() {
        return toString().getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Constructs a {@link Message} from its string representation.
     *
     * @param string The string representation of a {@link Message}.
     * @return That string, converted to a {@link Message}.
     * @throws ParseException If the string representation is invalid.
     */
    public static Message fromString(String string) throws ParseException {
        int barIndex = string.indexOf("|");
        int colonIndex = barIndex + string.substring(barIndex).indexOf(":");
        String dateString = string.substring(0, barIndex);
        String username = string.substring(barIndex + 2, colonIndex);
        String message = string.substring(colonIndex + 2);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        long timestamp = simpleDateFormat.parse(dateString).getTime();

        return new Message(username, message, timestamp);
    }

    /**
     * Converts an ASCII-encoded string (given as a byte array) to a string, and then converts that string to a {@link Message}.
     *
     * @param bytes ASCII-encoded string, as a byte array.
     * @return That string, converted to a {@link Message}.
     * @throws ParseException If the string representation is invalid.
     */
    public static Message fromStringBytes(byte[] bytes) throws ParseException {
        String string = new String(bytes, StandardCharsets.US_ASCII);
        return fromString(string);
    }



}
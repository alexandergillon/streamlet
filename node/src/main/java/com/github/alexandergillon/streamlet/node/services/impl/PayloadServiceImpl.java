package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.models.Message;
import com.github.alexandergillon.streamlet.node.services.PayloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;

/** Implementation of a {@link com.github.alexandergillon.streamlet.node.services.PayloadService}. */
@Service
@Slf4j
public class PayloadServiceImpl implements PayloadService {

    private final Set<Message> pendingMessages = Collections.synchronizedSet(new LinkedHashSet<>());

    @Override
    public void addPendingMessage(Message message) {
        pendingMessages.add(message);
    }

    @Override
    public void finalizedPayload(byte[] payload) {
        try {
            Message message = Message.fromStringBytes(payload);
            /* A block containing a message that does not appear in our pending messages is not necessarily an error -
            it is possible that we didn't hear about this proposed message. However, if this happens often, this may
            be a symptom of some other issue (e.g. network, Kafka, etc.), which is likely of interest. */
            if (!pendingMessages.remove(message)) log.info("Finalized message that was not pending: " + message);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] getNextPayload(Set<Block> unfinalizedSet) {
        try {
            // TODO: synchronization issues to do with finalizing a block right as we propose one
            HashSet<Message> alreadyIncluded = new HashSet<>();
            for (Block block : unfinalizedSet) {
                alreadyIncluded.add(Message.fromStringBytes(block.getPayload()));
            }

            for (Message message : pendingMessages) {
                if (!alreadyIncluded.contains(message)) {
                    return message.toStringBytes();
                }
            }

            return null;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}

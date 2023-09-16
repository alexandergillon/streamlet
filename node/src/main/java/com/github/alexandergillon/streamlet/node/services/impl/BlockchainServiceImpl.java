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

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.blockchain.Blockchain;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.InvalidBlockException;
import com.github.alexandergillon.streamlet.node.blockchain.exceptions.UnknownBlockException;
import com.github.alexandergillon.streamlet.node.blockchain.impl.memory.InMemoryBlockchain;
import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import com.github.alexandergillon.streamlet.node.services.KafkaSendingService;
import com.github.alexandergillon.streamlet.node.services.PayloadService;
import com.github.alexandergillon.streamlet.node.util.SerializationUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/** Implementation of a {@link BlockchainService}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainServiceImpl implements BlockchainService {

    // Constants from Spring properties
    @Value("${streamlet.node.id}")
    private int nodeId;
    @Value("${streamlet.participants}")
    private int numNodes;
    @Value("${streamlet.notarization.threshold}")
    private double notarizationProportion;

    // Autowired dependencies (via RequiredArgsConstructor)
    private final CryptographyService cryptographyService;
    private final PayloadService payloadService;
    private final KafkaSendingService kafkaSendingService;

    // Member variables
    private int currentEpoch = -1;
    private boolean firstProposalForEpoch = true;
    private Blockchain blockchain;

    /**
     * We need constants from Spring properties to instantiate the blockchain, so we do it
     * in a {@link PostConstruct}.
     */
    @PostConstruct
    private void initializeBlockchain() {
        blockchain = new InMemoryBlockchain(nodeId, (int)Math.ceil(numNodes * notarizationProportion), payloadService);
    }

    @Override
    public void setEpoch(int epoch) {
        if (epoch < 0) throw new IllegalArgumentException("Epoch " + epoch + " is less than zero");
        if (epoch <= currentEpoch) throw new IllegalArgumentException("Epoch " + epoch + " is less than current epoch of " + currentEpoch);
        currentEpoch = epoch;
        firstProposalForEpoch = true;
    }

    @Override
    public boolean processProposedBlock(Block block, int proposer, byte[] signature) {
        checkEpoch();
        // If the block is invalid, we discard it and return false
        if (!validateProposedBlock(block, proposer, signature)) return false;

        try {
            boolean votedOnBlock = blockchain.processProposedBlock(block, proposer, currentEpoch, firstProposalForEpoch);
            firstProposalForEpoch = false;
            return votedOnBlock;
        } catch (InvalidBlockException e) {
            log.warn("Received invalid block.", e);
            return false;
        } catch (UnknownBlockException e) {
            // TODO: handle orphaned blocks
            log.error("Unknown block.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processBlockVote(Block block, int voterId, byte[] signature, byte[] proposerSignature) {
        checkEpoch();
        // If the block is invalid, we discard it
        if (!validateVote(block, voterId, signature, proposerSignature)) return;

        try {
            blockchain.processBlockVote(block, voterId);
        } catch (InvalidBlockException e) {
            log.warn("Received invalid block.", e);
        } catch (UnknownBlockException e) {
            // TODO: handle orphaned blocks
            log.error("Unknown block.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Block> getFinalizedChain() {
        checkEpoch();
        return blockchain.getFinalizedChain();
    }

    @Override
    public void proposeBlock() {
        checkEpoch();

        Block parent = blockchain.getLongestNotarizedChainTail();
        Set<Block> unfinalizedSet = blockchain.getUnfinalizedAncestorSetOf(parent);
        byte[] payload = payloadService.getNextPayload(unfinalizedSet);

        if (payload == null) return;

        Block proposedBlock = new Block(parent.getHash(), currentEpoch, payload);
        kafkaSendingService.broadcast(SerializationUtils.buildProposeBroadcast(nodeId, proposedBlock, cryptographyService.signBase64(proposedBlock)));
    }

    /** Checks that the epoch has been set correctly, before other {@link BlockchainService} functions are called. */
    private void checkEpoch() {
        if (currentEpoch < 0) throw new IllegalStateException("Epoch of blockchain has not been set.");
    }

    /**
     * Performs a number of validations on a proposed block. For example, that the epoch is valid, signature
     * is valid, etc.
     *
     * @param block The block to validate.
     * @param proposer The proposer of the block.
     * @param signature The signature of the proposer on the block.
     * @return Whether the block is valid.
     */
    private boolean validateProposedBlock(Block block, int proposer, byte[] signature) {
        int leader = cryptographyService.leaderForEpoch(block.getEpoch());
        if (proposer != leader) {
            log.warn("Received proposed block from node {}, but node {} is leader for the block's epoch: {}", proposer, leader, block);
            return false;
        }

        if (!cryptographyService.validateProposal(block, signature)) {
            log.warn("Received proposed block whose signature could not be validated: {}", block);
            return false;
        }

        if (block.getEpoch() < 0 || block.getEpoch() > currentEpoch) {
            log.warn("Received proposed block with invalid epoch {}, current epoch {}: {}",  block.getEpoch(), currentEpoch, block);
            return false;
        }

        if (block.getParentHash().length != Block.SHA_256_HASH_LENGTH_BYTES) {
            log.warn("Received proposed block with parent invalid hash length {}: {}",  block.getParentHash().length, block);
            return false;
        }

        if (block.getPayload().length == 0) {
            log.warn("Received block with empty payload: {}", block);
            return false;
        }

        return true;
    }

    /**
     * Performs a number of validations on a vote. For example, that the epoch is valid, signature is valid, etc.
     *
     * @param block The voted-on block.
     * @param voterId ID of the node who voted on the block.
     * @param signature The digital signature of the voter on the block.
     * @param proposerSignature The digital signature of the original proposer on the block.
     * @return Whether the vote is valid.
     */
    private boolean validateVote(Block block, int voterId, byte[] signature, byte[] proposerSignature) {
        if (!cryptographyService.validateVote(block, voterId, signature)) {
            log.warn("Received vote on block whose signature could not be validated: {}", block);
            return false;
        }

        if (!cryptographyService.validateProposal(block, proposerSignature)) {
            log.warn("Received vote on block whose proposer signature could not be validated: {}", block);
            return false;
        }

        if (block.getEpoch() < 0 || block.getEpoch() > currentEpoch) {
            log.warn("Received vote on block with invalid epoch {}, current epoch {}: {}",  block.getEpoch(), currentEpoch, block);
            return false;
        }

        if (block.getParentHash().length != Block.SHA_256_HASH_LENGTH_BYTES) {
            log.warn("Received vote on block with parent invalid hash length {}: {}",  block.getParentHash().length, block);
            return false;
        }

        if (block.getPayload().length == 0) {
            log.warn("Received vote on block with empty payload: {}", block);
            return false;
        }

        return true;
    }
}

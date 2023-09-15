package com.github.alexandergillon.streamlet.node.controllers;

import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.util.SerializationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller to allow users / other web services to view the blockchain. */
@RestController
@RequiredArgsConstructor
public class BlockchainController {

    // Autowired dependencies (via RequiredArgsConstructor)
    private final BlockchainService blockchainService;

    /** @return The finalized message chain of the blockchain, as readable text. */
    @GetMapping(value = "/chain/readable", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getChainText() {
         return SerializationUtils.blockListMessagesToReadableText(blockchainService.getFinalizedChain());
    }

    /** @return The finalized message chain of the blockchain, as a JSON string. */
    @GetMapping(value = "/chain/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getChainJson() {
        return SerializationUtils.blockListMessagesToJson(blockchainService.getFinalizedChain());
    }

}

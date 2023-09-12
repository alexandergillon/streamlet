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

    /** @return The finalized chain of the blockchain, as a JSON string. */
    @GetMapping(value = "/chain/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getChainJson() {
        return SerializationUtils.blockListToJson(blockchainService.getFinalizedChain());
    }

}

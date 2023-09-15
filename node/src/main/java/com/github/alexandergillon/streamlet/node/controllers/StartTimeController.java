package com.github.alexandergillon.streamlet.node.controllers;

import com.github.alexandergillon.streamlet.node.services.TimingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Controller to tell this node when the first epoch starts. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class StartTimeController {

    // Autowired dependencies (via RequiredArgsConstructor)
    private final TimingService timingService;

    @GetMapping("/start")
    public String setStartTime(@RequestParam long time) {
        try {
            timingService.setStartTime(time);
            return "Start time set.";
        } catch (IllegalStateException e) {
            log.warn("Attempted to set start time, but start time has already been set.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time has already been set.");
        } catch (IllegalArgumentException e) {
            log.warn("IllegalArgumentException while setting start time: ", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid start time: " + time);
        }
    }

}

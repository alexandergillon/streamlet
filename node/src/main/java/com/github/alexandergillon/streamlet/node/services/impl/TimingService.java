package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Timing service to keep the blockchain's epoch up to date, and to prompt the blockchain to propose blocks when
 * it is the leader of the epoch. */
@Configuration
@EnableScheduling
@Service
@RequiredArgsConstructor
public class TimingService {

    // Constants from Spring properties
    @Value("${streamlet.node.id}")
    private int nodeId;
    @Value("${streamlet.epoch.start-time.value:0x7fffffffffffffff}")
    private long startTimeMillis;
    @Value("${streamlet.epoch.start-time.location:#{null}}")
    private String startTimePath;
    @Value("${streamlet.epoch.duration}")
    private long epochDurationMillis;

    // Autowired dependencies (via RequiredArgsConstructor)
    private final BlockchainService blockchainService;
    private final CryptographyService cryptographyService;
    private final Environment environment;

    // Member variables
    private boolean enabled = true;  // Timing is always enabled outside of unit tests
    private int lastEpoch = -1;

    /** Checks whether this service is enabled or not, and sets member variables accordingly. */
    @PostConstruct
    private void setup() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("unittests")) {
            // We are in a unit test. Check if timing is enabled.
            String timingEnabled = environment.resolvePlaceholders("${streamlet.testing.timing.enabled}");
            enabled = "true".equals(timingEnabled);
            // Regardless of whether we are testing timing, we want to skip checkStartTime in unit tests
            return;
        }

        checkStartTime();
    }

    /**
     * Checks whether the start time was set via either the {@code streamlet.epoch.start-time.value} or
     * {@code streamlet.epoch.start-time.location} properties. If the location property was set, fetches the start
     * time from the file. If neither were set, throws an exception, which will cause the application to fail to start.
     */
    private void checkStartTime() {
        if (startTimeMillis == -1 && startTimePath == null) {
            throw new IllegalArgumentException("Epoch start time cannot be determined: neither streamlet.epoch.start-time.value or streamlet.epoch.start-time.location were defined as properties");
        } else if (startTimeMillis == -1) {
            try {
                String startTimeString = Files.readString(Path.of(startTimePath));
                startTimeMillis = Long.parseLong(startTimeString);
            } catch (IOException e) {
                throw new RuntimeException("Error reading epoch start time from file", e);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Epoch start time in file is invalid", e);
            }
        }
    }

    /** Checks whether a new epoch has occured, and notifies the {@link BlockchainService} accordingly. */
    @Scheduled(fixedDelay = 20)
    private void tick() {
        if (!enabled) return;
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis < startTimeMillis) return;

        int epoch = (int) ((currentTimeMillis - startTimeMillis) / epochDurationMillis);
        if (epoch != lastEpoch) {
            blockchainService.setEpoch(epoch);
            lastEpoch = epoch;

            // TODO: move this check to BlockchainService
            if (epoch > 0 && cryptographyService.leaderForEpoch(epoch) == nodeId) {
                blockchainService.proposeBlock();
            }
        }
    }

}

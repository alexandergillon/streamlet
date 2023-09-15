package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import com.github.alexandergillon.streamlet.node.services.TimingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Timing service to keep the blockchain's epoch up to date, and to prompt the blockchain to propose blocks when
 * it is the leader of the epoch. */
@Configuration
@EnableScheduling
@Service
@RequiredArgsConstructor
public class TimingServiceImpl implements TimingService {

    // Constants from Spring properties
    @Value("${streamlet.node.id}")
    private int nodeId;
    @Value("${streamlet.epoch.duration}")
    private long epochDurationMillis;

    // Autowired dependencies (via RequiredArgsConstructor)
    private final BlockchainService blockchainService;
    private final CryptographyService cryptographyService;

    // Member variables
    private boolean started = false;
    private int lastEpoch = -1;
    private long startTimeMillis = 0x7fffffffffffffffL;

    @Override
    public void setStartTime(long startTimeMillis) {
        if (started) throw new IllegalStateException("Timing service has already started.");
        if (startTimeMillis < System.currentTimeMillis() - 60000) throw new IllegalArgumentException("Time has already passed.");

        started = true;
        this.startTimeMillis = startTimeMillis;
    }

    /** Checks whether a new epoch has occured, and notifies the {@link BlockchainService} accordingly. */
    @Scheduled(fixedDelay = 20)
    private void tick() {
        if (!started) return;
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

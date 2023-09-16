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

import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import com.github.alexandergillon.streamlet.node.services.TimingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
            log.info("Beginning epoch {}", epoch);
            blockchainService.setEpoch(epoch);
            lastEpoch = epoch;

            // TODO: move this check to BlockchainService
            if (epoch > 0 && cryptographyService.leaderForEpoch(epoch) == nodeId) {
                blockchainService.proposeBlock();
            }
        }
    }

}

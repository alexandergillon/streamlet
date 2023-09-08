package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.services.BlockchainService;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;


@SpringBootTest(properties = {"streamlet.testing.timing.enabled=true"})
class TimingServiceTest {

    @MockBean
    private CryptographyService cryptographyService;

    @MockBean
    private BlockchainService blockchainService;

    @Autowired
    private TimingService timingService;

    @Value("${streamlet.epoch.duration}")
    private long epochDurationMillis;

    @Value("${streamlet.node.id}")
    private int nodeId;

    private int epoch = -1;

    private long startTimeMillis = -1;

    // Tests that timing service updates epoch within 100ms of start of epoch
    private final long MAX_RESPONSE_TIME_MILLIS = 100;

    // Delay to give us time to get to test function after setup
    private final long START_TIME_DELAY_MILLIS = 3000;

    private final int NUM_EPOCHS = 10;

    private final int[] leaders = { -1, 1, 0, 2, 1, 2, 1, 3, 2, 1, 0 };

    @BeforeEach
    public void setup() {
        // setup cryptographyService mock
        Answer<Integer> leaderForEpochAnswer = invocationOnMock -> leaders[(int) invocationOnMock.getArgument(0)];
        when(cryptographyService.leaderForEpoch(anyInt())).thenAnswer(leaderForEpochAnswer);

        // setup blockchainService mock
        Answer<Void> setEpochAnswer = invocationOnMock -> {
            int epochArgument = invocationOnMock.getArgument(0);
            long currentTimeMillis = System.currentTimeMillis();
            assertFalse(epochArgument < 0);
            assertTrue(epochArgument > epoch);
            assertEquals((currentTimeMillis - startTimeMillis) / epochDurationMillis, epochArgument);
            // Tests that the last epoch was < maxResponseTimeMillis ago. floorDiv is because the number being divided could be negative and we want to round down
            assertEquals(epochArgument-1,  Math.floorDiv(currentTimeMillis - startTimeMillis - MAX_RESPONSE_TIME_MILLIS, epochDurationMillis));

            epoch = epochArgument;
            return null;
        };

        doAnswer(setEpochAnswer).when(blockchainService).setEpoch(anyInt());
        doNothing().when(blockchainService).proposeBlock();

        // mock start time
        startTimeMillis = System.currentTimeMillis() + START_TIME_DELAY_MILLIS;
        ReflectionTestUtils.setField(timingService, "startTimeMillis", startTimeMillis);
    }

    // Tests normal timing behavior: epoch is set in time for entire duration
    @Test
    public void testTiming() throws InterruptedException {
        assertTrue(System.currentTimeMillis() < startTimeMillis);
        long endTimeMillis = startTimeMillis + (epochDurationMillis * NUM_EPOCHS) - epochDurationMillis / 2;

        while (System.currentTimeMillis() < endTimeMillis) {
            //noinspection BusyWait
            Thread.sleep(100);
        }

        // number of times this block should have been proposer
        int numberOfProposals = (int) Arrays.stream(leaders).filter(x -> x == nodeId).count();

        verify(blockchainService, times(NUM_EPOCHS)).setEpoch(anyInt());
        verify(blockchainService, times(numberOfProposals)).proposeBlock();
        assertEquals(epoch, NUM_EPOCHS -1); // -1 because we start with epoch 0

    }

}
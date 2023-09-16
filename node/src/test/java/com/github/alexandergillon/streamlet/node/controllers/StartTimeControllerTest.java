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
package com.github.alexandergillon.streamlet.node.controllers;

import com.github.alexandergillon.streamlet.node.services.TimingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StartTimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private TimingService timingService;

    // Tests that setting start time works correctly
    @Test
    @DirtiesContext
    public void testSetStartTime() throws Exception {
        long startTime = System.currentTimeMillis() + 10000;
        mockMvc.perform(get("/start?time=" + startTime)).andExpect(status().isOk());
        verify(timingService).setStartTime(startTime);
    }

    // Tests that request with missing 'time' parameter is rejected
    @Test
    @DirtiesContext
    public void missingParameter() throws Exception {
        mockMvc.perform(get("/start")).andExpect(status().isBadRequest());
    }

    // Tests that time far in the past is rejected
    @Test
    @DirtiesContext
    public void testPassedTime() throws Exception {
        long startTime = System.currentTimeMillis() - 100000000;
        mockMvc.perform(get("/start?time=" + startTime)).andExpect(status().isBadRequest());
    }

    // Tests that second call to set start time is rejected
    @Test
    @DirtiesContext
    public void testTwoCalls() throws Exception {
        long startTime = System.currentTimeMillis() + 10000;
        mockMvc.perform(get("/start?time=" + startTime)).andExpect(status().isOk());
        verify(timingService).setStartTime(startTime);

        long secondStartTime = System.currentTimeMillis() + 100000;
        mockMvc.perform(get("/start?time=" + secondStartTime)).andExpect(status().isBadRequest());
    }

}
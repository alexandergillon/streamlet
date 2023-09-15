package com.github.alexandergillon.streamlet.node.services;

/** Service to handle timing of the blockchain. */
public interface TimingService {

    /**
     * Sets the start time. Can only be called once.
     *
     * @param startTimeMillis The start time of the first epoch.
     * @throws IllegalStateException If this method has already been called.
     * @throws IllegalArgumentException If startTimeMillis is invalid.
     */
    void setStartTime(long startTimeMillis) throws IllegalStateException, IllegalArgumentException;

}

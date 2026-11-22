package com.iitsm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides a simulated clock offset for demo purposes.
 * Allows fast-forwarding time to test SLA breaches and escalations.
 */
@Slf4j
@Service
public class DemoTimeService {

    private final AtomicLong offsetMinutes = new AtomicLong(0);

    /**
     * Returns the current "demo time" (actual time + offset).
     */
    public LocalDateTime now() {
        return LocalDateTime.now().plusMinutes(offsetMinutes.get());
    }

    /**
     * Advances the demo clock by the given number of minutes.
     */
    public long advanceTime(int minutes) {
        long newOffset = offsetMinutes.addAndGet(minutes);
        log.info("Demo time advanced by {} minutes. Total offset: {} minutes. Demo time: {}",
                minutes, newOffset, now());
        return newOffset;
    }

    /**
     * Resets the demo clock to real time.
     */
    public void resetTime() {
        offsetMinutes.set(0);
        log.info("Demo time reset to real time");
    }

    /**
     * Returns the current offset in minutes.
     */
    public long getOffsetMinutes() {
        return offsetMinutes.get();
    }
}

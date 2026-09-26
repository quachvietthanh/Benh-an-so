package com.benhsoan.infrastructure.time;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.time.ClockPort;

@Component
public class SystemClockAdapter
        implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }

}
package com.benhsoan.port.outbound.time;

import java.time.Instant;

public interface ClockPort {

    Instant now();

}
package com.benhsoan.port.dto.command.carelog;

import java.time.Instant;

import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.carelog.enums.ContactChannel;

public record SearchPostCareLogsQuery(
        ContactChannel channel,
        Instant from,
        Instant to,
        Pageable pageable
) {
}

package com.benhsoan.port.dto.command.queue;

import java.time.LocalDate;
import java.util.UUID;

public record GetQueueDisplayQuery(
        LocalDate queueDate,
        UUID roomId
) {}

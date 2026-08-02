package com.benhsoan.port.dto.command.queue;

import java.time.LocalDate;
import java.util.UUID;

public record GetQueuesQuery(LocalDate queueDate, UUID doctorId, UUID roomId) {
}

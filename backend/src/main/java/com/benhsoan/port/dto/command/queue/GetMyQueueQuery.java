package com.benhsoan.port.dto.command.queue;

import java.time.LocalDate;

public record GetMyQueueQuery(LocalDate queueDate) {
}

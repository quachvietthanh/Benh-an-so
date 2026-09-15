package com.benhsoan.port.dto.command.queue;

import java.util.UUID;

import com.benhsoan.domain.visit.enums.VisitCloseOutcome;

public record CloseVisitCommand(UUID queueItemId, VisitCloseOutcome outcome, String reason) {
}

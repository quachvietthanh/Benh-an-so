package com.benhsoan.port.dto.result;

import java.util.UUID;

public record AccessLogAccountCountResult(UUID accessedBy, long accessCount) {
}

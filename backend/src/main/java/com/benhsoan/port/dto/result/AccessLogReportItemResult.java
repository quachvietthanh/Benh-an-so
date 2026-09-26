package com.benhsoan.port.dto.result;

public record AccessLogReportItemResult(
        String username,
        String fullName,
        long accessCount
) {
}

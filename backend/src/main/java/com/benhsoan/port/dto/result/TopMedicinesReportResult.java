package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TopMedicinesReportResult(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        List<TopMedicineItemResult> items
) {
}

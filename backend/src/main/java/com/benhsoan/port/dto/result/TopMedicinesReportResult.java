package com.benhsoan.port.dto.result;

import java.time.LocalDate;
import java.util.List;

public record TopMedicinesReportResult(
        LocalDate from,
        LocalDate to,
        List<TopMedicineItemResult> items
) {
}

package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.TopMedicinesReportResult;

public interface GetTopMedicinesReportUseCase {

    TopMedicinesReportResult getTopMedicines(LocalDate from, LocalDate to);
}

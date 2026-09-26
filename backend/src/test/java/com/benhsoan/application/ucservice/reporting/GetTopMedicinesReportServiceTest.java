package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.port.dto.result.TopMedicineItemResult;
import com.benhsoan.port.dto.result.TopMedicinesReportResult;
import com.benhsoan.port.outbound.time.ClockPort;

class GetTopMedicinesReportServiceTest {

    @Test
    void addsGeneratedAtFromClockPortToReportResponseMetadata() {
        OperationalReportDataService operationalReportDataService = mock(OperationalReportDataService.class);
        ClockPort clockPort = mock(ClockPort.class);
        Instant generatedAt = Instant.parse("2026-08-03T10:15:30Z");

        when(operationalReportDataService.getTopMedicines(any(LocalDate.class), any(LocalDate.class))).thenReturn(new TopMedicinesReportResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                List.of(new TopMedicineItemResult(
                        1,
                        UUID.fromString("16000000-0000-0000-0000-000000000001"),
                        "MED-PARA-500",
                        "Paracetamol 500 mg",
                        9L))
        ));
        when(clockPort.now()).thenReturn(generatedAt);

        GetTopMedicinesReportService service = new GetTopMedicinesReportService(operationalReportDataService, clockPort);
        TopMedicinesReportResult result = service.getTopMedicines(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        assertEquals(generatedAt, result.generatedAt());
        assertEquals(1, result.items().get(0).rank());
    }
}

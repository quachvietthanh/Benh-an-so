package com.benhsoan.adapter.inbound.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalSummaryResponse;
import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalTimelineResponse;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;
import com.benhsoan.adapter.inbound.rest.response.reporting.TopMedicinesReportResponse;
import com.benhsoan.port.dto.result.TopMedicineItemResult;
import com.benhsoan.port.dto.result.TopMedicinesReportResult;

class ReportingRestMapperTest {

    private final ReportingRestMapper mapper = new ReportingRestMapper();

    @Test
    void mapsSummaryResultToStableResponseContract() {
        OperationalSummaryResponse response = mapper.toResponse(new OperationalSummaryResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                12L,
                new BigDecimal("5400000"),
                "VND"
        ));

        assertEquals(LocalDate.of(2026, 8, 1), response.from());
        assertEquals(LocalDate.of(2026, 8, 3), response.to());
        assertEquals(12L, response.visitCount());
        assertEquals(new BigDecimal("5400000"), response.revenue());
        assertEquals("VND", response.currency());
    }

    @Test
    void mapsTimelineResultToStableResponseContract() {
        OperationalTimelineResponse response = mapper.toResponse(new OperationalTimelineResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                List.of(
                        new OperationalTimelineItemResult(LocalDate.of(2026, 8, 1), 2L, new BigDecimal("100000")),
                        new OperationalTimelineItemResult(LocalDate.of(2026, 8, 2), 0L, BigDecimal.ZERO)
                )
        ));

        assertEquals(LocalDate.of(2026, 8, 1), response.from());
        assertEquals(LocalDate.of(2026, 8, 3), response.to());
        assertEquals(2, response.items().size());
        assertEquals(LocalDate.of(2026, 8, 1), response.items().get(0).date());
        assertEquals(2L, response.items().get(0).visitCount());
        assertEquals(new BigDecimal("100000"), response.items().get(0).revenue());
        assertEquals(LocalDate.of(2026, 8, 2), response.items().get(1).date());
        assertEquals(0L, response.items().get(1).visitCount());
        assertEquals(BigDecimal.ZERO, response.items().get(1).revenue());
    }

    @Test
    void mapsTopMedicinesResultToStableResponseContract() {
        TopMedicinesReportResponse response = mapper.toResponse(new TopMedicinesReportResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                Instant.parse("2026-08-03T10:15:30Z"),
                List.of(
                        new TopMedicineItemResult(
                                1,
                                java.util.UUID.fromString("16000000-0000-0000-0000-000000000001"),
                                "MED-PARA-500",
                                "Paracetamol 500 mg",
                                9L
                        )
                )
        ));

        assertEquals(LocalDate.of(2026, 8, 1), response.from());
        assertEquals(LocalDate.of(2026, 8, 3), response.to());
        assertEquals(Instant.parse("2026-08-03T10:15:30Z"), response.generatedAt());
        assertEquals(1, response.items().size());
        assertEquals(1, response.items().get(0).rank());
        assertEquals("MED-PARA-500", response.items().get(0).medicineCode());
        assertEquals(9L, response.items().get(0).totalDispensedQuantity());
    }
}

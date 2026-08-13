package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalSummaryResponse;
import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalTimelineItemResponse;
import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalTimelineResponse;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;
import com.benhsoan.adapter.inbound.rest.response.reporting.TopMedicineItemResponse;
import com.benhsoan.adapter.inbound.rest.response.reporting.TopMedicinesReportResponse;
import com.benhsoan.port.dto.result.TopMedicineItemResult;
import com.benhsoan.port.dto.result.TopMedicinesReportResult;

@Component
public class ReportingRestMapper {

    public OperationalSummaryResponse toResponse(OperationalSummaryResult result) {
        return new OperationalSummaryResponse(
                result.from(),
                result.to(),
                result.visitCount(),
                result.revenue(),
                result.currency()
        );
    }

    public OperationalTimelineResponse toResponse(OperationalTimelineResult result) {
        return new OperationalTimelineResponse(
                result.from(),
                result.to(),
                result.items().stream().map(this::toResponse).toList()
        );
    }

    public TopMedicinesReportResponse toResponse(TopMedicinesReportResult result) {
        return new TopMedicinesReportResponse(
                result.from(),
                result.to(),
                result.items().stream().map(this::toResponse).toList()
        );
    }

    private OperationalTimelineItemResponse toResponse(OperationalTimelineItemResult result) {
        return new OperationalTimelineItemResponse(
                result.date(),
                result.visitCount(),
                result.revenue()
        );
    }

    private TopMedicineItemResponse toResponse(TopMedicineItemResult result) {
        return new TopMedicineItemResponse(
                result.medicineId(),
                result.medicineCode(),
                result.medicineName(),
                result.totalDispensedQuantity()
        );
    }
}

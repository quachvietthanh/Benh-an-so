package com.benhsoan.application.ucservice.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.port.dto.result.PayableEncounterResult;
import com.benhsoan.port.outbound.repository.billing.PayableEncounterSummary;

@Component
public class PayableEncounterResultMapper {

    public PayableEncounterResult toResult(PayableEncounterSummary summary) {
        return new PayableEncounterResult(
                summary.visitId(),
                summary.visitCode(),
                summary.patientId(),
                summary.patientCode(),
                summary.patientName(),
                summary.reason(),
                summary.completedAt()
        );
    }
}

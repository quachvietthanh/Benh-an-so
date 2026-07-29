package com.benhsoan.adapter.inbound.rest.mapper;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.patient.MedicalHistoryItemResponse;
import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;
import com.benhsoan.port.dto.result.MedicalHistoryItemResult;

@Component
public class MedicalHistoryRestMapper {

    public GetPatientMedicalHistoryQuery toQuery(
            UUID patientId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        return new GetPatientMedicalHistoryQuery(patientId, from, to, page, size);
    }

    public Page<MedicalHistoryItemResponse> toResponse(Page<MedicalHistoryItemResult> resultPage) {
        return resultPage.map(this::toResponse);
    }

    private MedicalHistoryItemResponse toResponse(MedicalHistoryItemResult result) {
        return new MedicalHistoryItemResponse(
                result.visitId(),
                result.visitCode(),
                result.visitType(),
                result.visitStatus(),
                result.visitAt(),
                result.startedAt(),
                result.completedAt(),
                result.reason(),
                result.note(),
                result.doctorId(),
                result.doctorName(),
                result.medicalRecordId(),
                result.medicalRecordStatus(),
                result.chiefComplaint(),
                result.conclusion()
        );
    }
}

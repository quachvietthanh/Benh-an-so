package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalClinicalResultDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalClinicalResultDetailResponse.AttachmentResponse;
import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalClinicalResultSummaryResponse;
import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultDetailResult;
import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultSummaryResult;

@Component
public class PatientPortalClinicalResultRestMapper {

    public PatientPortalClinicalResultSummaryResponse toSummaryResponse(PatientPortalClinicalResultSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new PatientPortalClinicalResultSummaryResponse(
                result.clinicalResultId(),
                result.clinicalOrderItemId(),
                result.visitId(),
                result.serviceCode(),
                result.serviceName(),
                result.resultType(),
                result.numericValue(),
                result.lowerBound(),
                result.upperBound(),
                result.textValue(),
                result.unit(),
                result.referenceRange(),
                result.abnormalFlag(),
                result.conclusion(),
                result.status(),
                result.doctorName(),
                result.enteredAt(),
                result.hasAttachment()
        );
    }

    public PatientPortalClinicalResultDetailResponse toDetailResponse(PatientPortalClinicalResultDetailResult result) {
        if (result == null) {
            return null;
        }
        List<AttachmentResponse> attachments = result.attachments() == null ? List.of() : result.attachments().stream()
                .map(att -> new AttachmentResponse(
                        att.attachmentId(),
                        att.fileName(),
                        att.contentType(),
                        att.fileSize(),
                        att.attachmentType()
                ))
                .toList();

        return new PatientPortalClinicalResultDetailResponse(
                result.clinicalResultId(),
                result.clinicalOrderItemId(),
                result.visitId(),
                result.visitCode(),
                result.visitAt(),
                result.serviceCode(),
                result.serviceName(),
                result.resultType(),
                result.numericValue(),
                result.lowerBound(),
                result.upperBound(),
                result.textValue(),
                result.unit(),
                result.referenceRange(),
                result.abnormalFlag(),
                result.conclusion(),
                result.status(),
                result.orderingDoctorName(),
                result.performingDoctorName(),
                result.specialtyName(),
                result.enteredAt(),
                attachments
        );
    }
}

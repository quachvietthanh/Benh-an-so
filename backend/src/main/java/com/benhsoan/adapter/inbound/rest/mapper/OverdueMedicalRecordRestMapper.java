package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.medicalrecord.OverdueMedicalRecordResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.SigningReminderResponse;
import com.benhsoan.port.dto.result.OverdueMedicalRecordResult;
import com.benhsoan.port.dto.result.SigningReminderResult;

@Component
public class OverdueMedicalRecordRestMapper {

    public OverdueMedicalRecordResponse toResponse(OverdueMedicalRecordResult result) {
        if (result == null) {
            return null;
        }
        return new OverdueMedicalRecordResponse(
                result.medicalRecordId(),
                result.status(),
                result.visitId(),
                result.visitCode(),
                result.visitCompletedAt(),
                result.patientId(),
                result.patientCode(),
                result.patientFullName(),
                result.doctorId(),
                result.doctorFullName(),
                result.doctorEmail(),
                result.doctorPhone(),
                result.signingDeadlineHours(),
                result.deadlineAt(),
                result.overdueHours(),
                result.reminderCount(),
                result.lastRemindedAt()
        );
    }

    public Page<OverdueMedicalRecordResponse> toResponsePage(Page<OverdueMedicalRecordResult> page) {
        if (page == null) {
            return Page.empty();
        }
        return page.map(this::toResponse);
    }

    public SigningReminderResponse toResponse(SigningReminderResult result) {
        if (result == null) {
            return null;
        }
        return new SigningReminderResponse(
                result.id(),
                result.medicalRecordId(),
                result.doctorId(),
                result.doctorFullName(),
                result.remindedBy(),
                result.remindedByName(),
                result.remindedAt(),
                result.overdueHours(),
                result.channel(),
                result.notes(),
                result.status()
        );
    }
}

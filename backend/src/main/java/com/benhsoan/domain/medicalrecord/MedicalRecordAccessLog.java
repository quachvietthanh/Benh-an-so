package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalRecordAccessLog {

    private UUID id, patientId, visitId, medicalRecordId, accessedBy;
    private MedicalRecordAccessAction action;
    private String detail, ipAddress;
    private Instant accessedAt;

    private MedicalRecordAccessLog(UUID id, UUID patientId, UUID visitId, UUID recordId, UUID by, MedicalRecordAccessAction action, String detail, Instant at) {
        this.id = Objects.requireNonNull(id);
        this.patientId = Objects.requireNonNull(patientId);
        this.visitId = visitId;
        medicalRecordId = recordId;
        accessedBy = Objects.requireNonNull(by);
        this.action = Objects.requireNonNull(action);
        this.detail = detail;
        ipAddress = null;
        accessedAt = Objects.requireNonNull(at);
    }

    public static MedicalRecordAccessLog createRecordAccess(UUID patientId, UUID visitId, UUID recordId, UUID by, MedicalRecordAccessAction action, String detail, Instant accessedAt) {
        if (action == MedicalRecordAccessAction.VIEW_HISTORY) {
            throw new ValidationException("Record access cannot use VIEW_HISTORY.");
        
        }if (visitId == null || recordId == null) {
            throw new ValidationException("Record access requires visit and medical record.");
        
        }return new MedicalRecordAccessLog(UUID.randomUUID(), patientId, visitId, recordId, by, action, detail, Objects.requireNonNull(accessedAt));
    }

    public static MedicalRecordAccessLog createHistoryView(UUID patientId, UUID by, String detail, Instant accessedAt) {
        return new MedicalRecordAccessLog(UUID.randomUUID(), patientId, null, null, by, MedicalRecordAccessAction.VIEW_HISTORY, detail, accessedAt);
    }

    public static MedicalRecordAccessLog restore(UUID id, UUID patientId, UUID visitId, UUID recordId, UUID by, MedicalRecordAccessAction action, String detail, Instant at) {
        return new MedicalRecordAccessLog(id, patientId, visitId, recordId, by, action, detail, at);
    }
}

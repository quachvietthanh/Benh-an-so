package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalRecord {

    private UUID id, visitId, lockedBy, createdBy, updatedBy;
    private String chiefComplaint, symptoms, medicalHistory, physicalExamination, clinicalProgress, treatmentPlan, doctorInstructions, conclusion;
    private MedicalRecordStatus status;
    private Instant lockedAt, createdAt, updatedAt;

    private MedicalRecord(UUID id, UUID visitId, String a, String b, String c, String d, String e, String f, String g, String h, MedicalRecordStatus status, Instant lockedAt, UUID lockedBy, UUID createdBy, Instant createdAt, UUID updatedBy, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.visitId = Objects.requireNonNull(visitId);
        chiefComplaint = a;
        symptoms = b;
        medicalHistory = c;
        physicalExamination = d;
        clinicalProgress = e;
        treatmentPlan = f;
        doctorInstructions = g;
        conclusion = h;
        this.status = Objects.requireNonNull(status);
        this.lockedAt = lockedAt;
        this.lockedBy = lockedBy;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public static MedicalRecord create(UUID visitId, String a, String b, String c, String d, String e, String f, String g, String h, UUID createdBy) {
        return new MedicalRecord(UUID.randomUUID(), visitId, a, b, c, d, e, f, g, h, MedicalRecordStatus.DRAFT, null, null, createdBy, Instant.now(), null, null);
    }

    public static MedicalRecord restore(UUID id, UUID visitId, String a, String b, String c, String d, String e, String f, String g, String h, MedicalRecordStatus s, Instant l, UUID lb, UUID cb, Instant ca, UUID ub, Instant ua) {
        return new MedicalRecord(id, visitId, a, b, c, d, e, f, g, h, s, l, lb, cb, ca, ub, ua);
    }

    public void open(UUID by, Instant at) {
        if (status != MedicalRecordStatus.DRAFT) {
            throw new ValidationException("Only draft records can be opened.");
        
        }status = MedicalRecordStatus.OPEN;
        updatedBy = Objects.requireNonNull(by);
        updatedAt = Objects.requireNonNull(at);
    }

    public void updateContent(String a, String b, String c, String d, String e, String f, String g, String h, UUID by, Instant at) {
        ensureEditable();
        chiefComplaint = a;
        symptoms = b;
        medicalHistory = c;
        physicalExamination = d;
        clinicalProgress = e;
        treatmentPlan = f;
        doctorInstructions = g;
        conclusion = h;
        updatedBy = Objects.requireNonNull(by);
        updatedAt = Objects.requireNonNull(at);
    }

    public void lock(UUID by, Instant at) {
        ensureEditable();
        status = MedicalRecordStatus.LOCKED;
        lockedBy = updatedBy = Objects.requireNonNull(by);
        lockedAt = updatedAt = Objects.requireNonNull(at);
    }

    public boolean isLocked() {
        return status == MedicalRecordStatus.LOCKED;
    }

    public void ensureEditable() {
        if (isLocked()) {
            throw new ValidationException("Locked medical records cannot be updated.");
    
        }}
}

package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidStatusException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotSignedException;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalRecord {

    private UUID id, visitId, signedBy, lockedBy, createdBy, updatedBy, appliedTemplateVersionId, templateAppliedBy;
    private String chiefComplaint, symptoms, medicalHistory, physicalExamination, clinicalProgress, treatmentPlan, doctorInstructions, conclusion;
    private String signatureData;
    private MedicalRecordStatus status;
    private Instant signedAt, lockedAt, createdAt, updatedAt, templateAppliedAt;

    private MedicalRecord(
            UUID id,
            UUID visitId,
            String chiefComplaint,
            String symptoms,
            String medicalHistory,
            String physicalExamination,
            String clinicalProgress,
            String treatmentPlan,
            String doctorInstructions,
            String conclusion,
            MedicalRecordStatus status,
            String signatureData,
            Instant signedAt,
            UUID signedBy,
            Instant lockedAt,
            UUID lockedBy,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt,
            UUID appliedTemplateVersionId,
            UUID templateAppliedBy,
            Instant templateAppliedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.visitId = Objects.requireNonNull(visitId);
        this.chiefComplaint = chiefComplaint;
        this.symptoms = symptoms;
        this.medicalHistory = medicalHistory;
        this.physicalExamination = physicalExamination;
        this.clinicalProgress = clinicalProgress;
        this.treatmentPlan = treatmentPlan;
        this.doctorInstructions = doctorInstructions;
        this.conclusion = conclusion;
        this.status = Objects.requireNonNull(status);
        this.signatureData = signatureData;
        this.signedAt = signedAt;
        this.signedBy = signedBy;
        this.lockedAt = lockedAt;
        this.lockedBy = lockedBy;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.appliedTemplateVersionId = appliedTemplateVersionId;
        this.templateAppliedBy = templateAppliedBy;
        this.templateAppliedAt = templateAppliedAt;
    }

    public static MedicalRecord create(
            UUID visitId,
            String chiefComplaint,
            String symptoms,
            String medicalHistory,
            String physicalExamination,
            String clinicalProgress,
            String treatmentPlan,
            String doctorInstructions,
            String conclusion,
            UUID createdBy,
            Instant createdAt
    ) {
        return new MedicalRecord(
                UUID.randomUUID(),
                visitId,
                chiefComplaint,
                symptoms,
                medicalHistory,
                physicalExamination,
                clinicalProgress,
                treatmentPlan,
                doctorInstructions,
                conclusion,
                MedicalRecordStatus.DRAFT,
                null,
                null,
                null,
                null,
                null,
                createdBy,
                Objects.requireNonNull(createdAt),
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MedicalRecord restore(
            UUID id,
            UUID visitId,
            String chiefComplaint,
            String symptoms,
            String medicalHistory,
            String physicalExamination,
            String clinicalProgress,
            String treatmentPlan,
            String doctorInstructions,
            String conclusion,
            MedicalRecordStatus status,
            String signatureData,
            Instant signedAt,
            UUID signedBy,
            Instant lockedAt,
            UUID lockedBy,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        return new MedicalRecord(
                id,
                visitId,
                chiefComplaint,
                symptoms,
                medicalHistory,
                physicalExamination,
                clinicalProgress,
                treatmentPlan,
                doctorInstructions,
                conclusion,
                status,
                signatureData,
                signedAt,
                signedBy,
                lockedAt,
                lockedBy,
                createdBy,
                createdAt,
                updatedBy,
                updatedAt,
                null,
                null,
                null
        );
    }

    public static MedicalRecord restore(
            UUID id, UUID visitId, String chiefComplaint, String symptoms, String medicalHistory,
            String physicalExamination, String clinicalProgress, String treatmentPlan, String doctorInstructions,
            String conclusion, MedicalRecordStatus status, String signatureData, Instant signedAt, UUID signedBy,
            Instant lockedAt, UUID lockedBy, UUID createdBy, Instant createdAt, UUID updatedBy, Instant updatedAt,
            UUID appliedTemplateVersionId, UUID templateAppliedBy, Instant templateAppliedAt
    ) {
        return new MedicalRecord(id, visitId, chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion, status, signatureData, signedAt,
                signedBy, lockedAt, lockedBy, createdBy, createdAt, updatedBy, updatedAt, appliedTemplateVersionId,
                templateAppliedBy, templateAppliedAt);
    }

    public static MedicalRecord restore(
            UUID id,
            UUID visitId,
            String chiefComplaint,
            String symptoms,
            String medicalHistory,
            String physicalExamination,
            String clinicalProgress,
            String treatmentPlan,
            String doctorInstructions,
            String conclusion,
            MedicalRecordStatus status,
            String signatureData,
            Instant lockedAt,
            UUID lockedBy,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        return new MedicalRecord(
                id,
                visitId,
                chiefComplaint,
                symptoms,
                medicalHistory,
                physicalExamination,
                clinicalProgress,
                treatmentPlan,
                doctorInstructions,
                conclusion,
                status,
                signatureData,
                null,
                null,
                lockedAt,
                lockedBy,
                createdBy,
                createdAt,
                updatedBy,
                updatedAt,
                null,
                null,
                null
        );
    }

    public static MedicalRecord restore(
            UUID id,
            UUID visitId,
            String chiefComplaint,
            String symptoms,
            String medicalHistory,
            String physicalExamination,
            String clinicalProgress,
            String treatmentPlan,
            String doctorInstructions,
            String conclusion,
            MedicalRecordStatus status,
            Instant lockedAt,
            UUID lockedBy,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        return new MedicalRecord(
                id,
                visitId,
                chiefComplaint,
                symptoms,
                medicalHistory,
                physicalExamination,
                clinicalProgress,
                treatmentPlan,
                doctorInstructions,
                conclusion,
                status,
                null,
                null,
                null,
                lockedAt,
                lockedBy,
                createdBy,
                createdAt,
                updatedBy,
                updatedAt,
                null,
                null,
                null
        );
    }

    public void applyTemplateVersion(UUID templateVersionId, UUID appliedBy, Instant appliedAt) {
        ensureEditable();
        this.appliedTemplateVersionId = Objects.requireNonNull(templateVersionId);
        this.templateAppliedBy = Objects.requireNonNull(appliedBy);
        this.templateAppliedAt = Objects.requireNonNull(appliedAt);
        this.updatedBy = appliedBy;
        this.updatedAt = appliedAt;
    }

    public void open(UUID by, Instant at) {
        if (status != MedicalRecordStatus.DRAFT) {
            conflict("Only draft records can be opened.");
        }
        status = MedicalRecordStatus.OPEN;
        updatedBy = Objects.requireNonNull(by);
        updatedAt = Objects.requireNonNull(at);
    }

    public void updateContent(
            String chiefComplaint,
            String symptoms,
            String medicalHistory,
            String physicalExamination,
            String clinicalProgress,
            String treatmentPlan,
            String doctorInstructions,
            String conclusion,
            UUID by,
            Instant at
    ) {
        ensureEditable();
        this.chiefComplaint = chiefComplaint;
        this.symptoms = symptoms;
        this.medicalHistory = medicalHistory;
        this.physicalExamination = physicalExamination;
        this.clinicalProgress = clinicalProgress;
        this.treatmentPlan = treatmentPlan;
        this.doctorInstructions = doctorInstructions;
        this.conclusion = conclusion;
        this.updatedBy = Objects.requireNonNull(by);
        this.updatedAt = Objects.requireNonNull(at);
    }

    public void sign(String signatureData, UUID doctorId, Instant at) {
        if (isContentLocked()) {
            throw new MedicalRecordAlreadyLockedException();
        }
        ensureLockableContent();
        this.status = MedicalRecordStatus.SIGNED;
        this.signatureData = signatureData;
        this.signedBy = Objects.requireNonNull(doctorId);
        this.signedAt = Objects.requireNonNull(at);
        this.updatedBy = doctorId;
        this.updatedAt = at;
    }

    public void lock(UUID by, Instant at) {
        if (this.status == MedicalRecordStatus.LOCKED) {
            throw new MedicalRecordAlreadyLockedException();
        }
        if (this.status != MedicalRecordStatus.SIGNED) {
            throw new MedicalRecordNotSignedException(this.id);
        }
        this.status = MedicalRecordStatus.LOCKED;
        this.lockedBy = Objects.requireNonNull(by);
        this.lockedAt = Objects.requireNonNull(at);
        this.updatedBy = by;
        this.updatedAt = at;
    }

    public boolean isSigned() {
        return status == MedicalRecordStatus.SIGNED;
    }

    public boolean isLocked() {
        return status == MedicalRecordStatus.LOCKED;
    }

    public boolean isArchived() {
        return status == MedicalRecordStatus.ARCHIVED;
    }

    public boolean isContentLocked() {
        return isSigned() || isLocked() || isArchived();
    }

    public void archive(UUID by, Instant at) {
        if (status != MedicalRecordStatus.LOCKED) {
            conflict("Only locked records can be archived.");
        }
        status = MedicalRecordStatus.ARCHIVED;
        updatedBy = Objects.requireNonNull(by);
        updatedAt = Objects.requireNonNull(at);
    }

    public void ensureEditable() {
        if (isContentLocked()) {
            throw new MedicalRecordAlreadyLockedException();
        }
    }

    private void ensureLockableContent() {
        if (chiefComplaint == null || chiefComplaint.isBlank()) {
            throw new ValidationException("Chief complaint is required before locking medical record.");
        }
        if (conclusion == null || conclusion.isBlank()) {
            throw new ValidationException("Conclusion is required before locking medical record.");
        }
    }

    private void conflict(String message) {
        if (isContentLocked()) {
            throw new MedicalRecordAlreadyLockedException();
        }
        throw new MedicalRecordInvalidStatusException(message);
    }
}

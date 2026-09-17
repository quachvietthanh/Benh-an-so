package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
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
    private LocalDate revisitDate;
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
            LocalDate revisitDate,
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
        this.revisitDate = revisitDate;
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
        return create(
                visitId, chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion,
                null, null, createdBy, createdAt
        );
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
            LocalDate revisitDate,
            UUID createdBy,
            Instant createdAt
    ) {
        return create(
                visitId, chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion,
                revisitDate, null, createdBy, createdAt
        );
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
            LocalDate revisitDate,
            LocalDate visitDate,
            UUID createdBy,
            Instant createdAt
    ) {
        validateRevisitDate(revisitDate, visitDate);
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
                revisitDate,
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
        return restore(
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
                null,
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
        return restore(id, visitId, chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion, null, status, signatureData, signedAt,
                signedBy, lockedAt, lockedBy, createdBy, createdAt, updatedBy, updatedAt, appliedTemplateVersionId,
                templateAppliedBy, templateAppliedAt);
    }

    public static MedicalRecord restore(
            UUID id, UUID visitId, String chiefComplaint, String symptoms, String medicalHistory,
            String physicalExamination, String clinicalProgress, String treatmentPlan, String doctorInstructions,
            String conclusion, LocalDate revisitDate, MedicalRecordStatus status, String signatureData, Instant signedAt, UUID signedBy,
            Instant lockedAt, UUID lockedBy, UUID createdBy, Instant createdAt, UUID updatedBy, Instant updatedAt,
            UUID appliedTemplateVersionId, UUID templateAppliedBy, Instant templateAppliedAt
    ) {
        return new MedicalRecord(id, visitId, chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion, revisitDate, status, signatureData, signedAt,
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
        return restore(
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
                null,
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
        return restore(
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
                null,
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

    public boolean hasClinicalContent() {
        return hasText(chiefComplaint) || hasText(symptoms) || hasText(medicalHistory)
                || hasText(physicalExamination) || hasText(clinicalProgress) || hasText(treatmentPlan)
                || hasText(doctorInstructions) || hasText(conclusion) || revisitDate != null;
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
        updateContent(chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion,
                this.revisitDate, null, by, at);
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
            LocalDate revisitDate,
            LocalDate visitDate,
            UUID by,
            Instant at
    ) {
        ensureEditable();
        validateRevisitDate(revisitDate, visitDate);
        this.chiefComplaint = chiefComplaint;
        this.symptoms = symptoms;
        this.medicalHistory = medicalHistory;
        this.physicalExamination = physicalExamination;
        this.clinicalProgress = clinicalProgress;
        this.treatmentPlan = treatmentPlan;
        this.doctorInstructions = doctorInstructions;
        this.conclusion = conclusion;
        this.revisitDate = revisitDate;
        this.updatedBy = Objects.requireNonNull(by);
        this.updatedAt = Objects.requireNonNull(at);
    }

    public void updateInstructionsAndTreatmentPlan(
            String treatmentPlan,
            String doctorInstructions,
            LocalDate revisitDate,
            LocalDate visitDate,
            UUID by,
            Instant at
    ) {
        ensureEditable();
        validateRevisitDate(revisitDate, visitDate);
        this.treatmentPlan = treatmentPlan;
        this.doctorInstructions = doctorInstructions;
        this.revisitDate = revisitDate;
        this.updatedBy = Objects.requireNonNull(by);
        this.updatedAt = Objects.requireNonNull(at);
    }

    private static void validateRevisitDate(LocalDate revisitDate, LocalDate visitDate) {
        if (revisitDate != null && visitDate != null && revisitDate.isBefore(visitDate)) {
            throw new ValidationException("Ngày tái khám không được trước ngày khám.");
        }
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

    /**
     * Checks the immutable version that was applied to this record. This is deliberately
     * called only when signing: a draft may be incomplete while the doctor is working.
     */
    public void ensureRequiredTemplateSections(MedicalRecordTemplateVersion templateVersion) {
        Objects.requireNonNull(templateVersion, "templateVersion");
        for (MedicalRecordTemplateSection section : templateVersion.getSections()) {
            if (section.isRequired() && !hasText(valueOf(section.getFieldCode()))) {
                throw new ValidationException("Required template section is missing: " + section.getFieldCode());
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String valueOf(MedicalRecordFieldCode fieldCode) {
        return switch (fieldCode) {
            case CHIEF_COMPLAINT -> chiefComplaint;
            case SYMPTOMS -> symptoms;
            case MEDICAL_HISTORY -> medicalHistory;
            case PHYSICAL_EXAMINATION -> physicalExamination;
            case CLINICAL_PROGRESS -> clinicalProgress;
            case TREATMENT_PLAN -> treatmentPlan;
            case DOCTOR_INSTRUCTIONS -> doctorInstructions;
            case CONCLUSION -> conclusion;
        };
    }
}

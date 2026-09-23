package com.benhsoan.domain.patient;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.ConsentHistoryStatus;
import com.benhsoan.domain.patient.enums.ConsentScope;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.Getter;

/**
 * Domain entity đại diện cho một phiên bản phiếu đồng ý trong lịch sử (NCL-15-CN-005 / AC-02).
 */
@Getter
public class PatientConsentRecord {

    private final UUID id;
    private final UUID patientId;
    private final int versionNumber;
    private final String versionCode;
    private final ConsentHistoryStatus status;
    private final Set<ConsentScope> scopes;
    private final boolean consentAgreed;
    private final Instant consentAgreedAt;
    private final boolean consentWithdrawn;
    private final Instant consentWithdrawnAt;
    private final String consentWithdrawnReason;
    private final boolean nonMedicalUseRestricted;
    private final String signerName;
    private final UUID createdBy;
    private final Instant createdAt;

    private PatientConsentRecord(
            UUID id,
            UUID patientId,
            int versionNumber,
            String versionCode,
            ConsentHistoryStatus status,
            Set<ConsentScope> scopes,
            boolean consentAgreed,
            Instant consentAgreedAt,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted,
            String signerName,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "ID bản ghi consent không được null");
        this.patientId = Objects.requireNonNull(patientId, "ID bệnh nhân không được null");
        if (versionNumber <= 0) {
            throw new ValidationException("Số thứ tự phiên bản phải lớn hơn 0.");
        }
        this.versionNumber = versionNumber;
        this.versionCode = (versionCode != null && !versionCode.isBlank()) ? versionCode : "v1.0";
        this.status = Objects.requireNonNull(status, "Trạng thái consent không được null");
        this.scopes = scopes != null ? Collections.unmodifiableSet(scopes) : Collections.emptySet();
        this.consentAgreed = consentAgreed;
        this.consentAgreedAt = consentAgreedAt;
        this.consentWithdrawn = consentWithdrawn;
        this.consentWithdrawnAt = consentWithdrawnAt;
        this.consentWithdrawnReason = consentWithdrawnReason;
        this.nonMedicalUseRestricted = nonMedicalUseRestricted;
        this.signerName = signerName;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static PatientConsentRecord create(
            UUID patientId,
            int versionNumber,
            String versionCode,
            ConsentHistoryStatus status,
            Set<ConsentScope> scopes,
            boolean consentAgreed,
            Instant consentAgreedAt,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted,
            String signerName,
            UUID createdBy,
            Instant createdAt
    ) {
        return new PatientConsentRecord(
                UUID.randomUUID(),
                patientId,
                versionNumber,
                versionCode,
                status,
                scopes,
                consentAgreed,
                consentAgreedAt,
                consentWithdrawn,
                consentWithdrawnAt,
                consentWithdrawnReason,
                nonMedicalUseRestricted,
                signerName,
                createdBy,
                createdAt != null ? createdAt : Instant.now()
        );
    }

    public static PatientConsentRecord restore(
            UUID id,
            UUID patientId,
            int versionNumber,
            String versionCode,
            ConsentHistoryStatus status,
            Set<ConsentScope> scopes,
            boolean consentAgreed,
            Instant consentAgreedAt,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted,
            String signerName,
            UUID createdBy,
            Instant createdAt
    ) {
        return new PatientConsentRecord(
                id,
                patientId,
                versionNumber,
                versionCode,
                status,
                scopes,
                consentAgreed,
                consentAgreedAt,
                consentWithdrawn,
                consentWithdrawnAt,
                consentWithdrawnReason,
                nonMedicalUseRestricted,
                signerName,
                createdBy,
                createdAt
        );
    }
}

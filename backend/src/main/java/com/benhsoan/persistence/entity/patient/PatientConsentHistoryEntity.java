package com.benhsoan.persistence.entity.patient;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.ConsentHistoryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patient_consent_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientConsentHistoryEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "version_code", nullable = false, length = 30)
    private String versionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ConsentHistoryStatus status;

    @Column(name = "scopes", nullable = false, length = 500)
    private String scopes;

    @Column(name = "consent_agreed", nullable = false)
    private boolean consentAgreed;

    @Column(name = "consent_agreed_at")
    private Instant consentAgreedAt;

    @Column(name = "consent_withdrawn", nullable = false)
    private boolean consentWithdrawn;

    @Column(name = "consent_withdrawn_at")
    private Instant consentWithdrawnAt;

    @Column(name = "consent_withdrawn_reason", length = 500)
    private String consentWithdrawnReason;

    @Column(name = "non_medical_use_restricted", nullable = false)
    private boolean nonMedicalUseRestricted;

    @Column(name = "signer_name", length = 255)
    private String signerName;

    @Column(name = "created_by", columnDefinition = "BINARY(16)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

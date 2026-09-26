package com.benhsoan.persistence.entity.patient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.enums.PatientStatus;
import com.benhsoan.domain.patient.enums.PregnancyStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "patient_code", nullable = false, unique = true, length = 30)
    private String patientCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "identity_number", unique = true, length = 20)
    private String identityNumber;

    @Column(name = "insurance_number", length = 30)
    private String insuranceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", length = 20)
    private BloodType bloodType;

    @Column(name = "emergency_contact", length = 100)
    private String emergencyContact;

    @Column(name = "emergency_relationship", length = 50)
    private String emergencyRelationship;

    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;

    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Column(name = "guardian_relationship", length = 50)
    private String guardianRelationship;

    @Column(name = "guardian_phone", length = 20)
    private String guardianPhone;

    @Column(name = "guardian_identity_number", length = 20)
    private String guardianIdentityNumber;

    @Column(name = "guardian_user_id", columnDefinition = "BINARY(16)")
    private UUID guardianUserId;

    @Column(name = "consent_signer_name", length = 100)
    private String consentSignerName;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;

    @Column(name = "consent_agreed", nullable = false)
    private boolean consentAgreed;

    @Column(name = "consent_agreed_at")
    private Instant consentAgreedAt;

    @Column(name = "consent_version", length = 30)
    private String consentVersion;

    @Column(name = "consent_withdrawn", nullable = false)
    private boolean consentWithdrawn;

    @Column(name = "consent_withdrawn_at")
    private Instant consentWithdrawnAt;

    @Column(name = "consent_withdrawn_reason", length = 500)
    private String consentWithdrawnReason;

    @Column(name = "non_medical_use_restricted", nullable = false)
    private boolean nonMedicalUseRestricted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PatientStatus status = PatientStatus.ACTIVE;

    @Column(name = "merged_into_patient_id", columnDefinition = "BINARY(16)")
    private UUID mergedIntoPatientId;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "merged_by", columnDefinition = "BINARY(16)")
    private UUID mergedBy;

    @Column(name = "merge_reason", length = 500)
    private String mergeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "pregnancy_status", length = 30)
    private PregnancyStatus pregnancyStatus;

    @PrePersist
    void prePersist() {
        if (this.status == null) {
            this.status = PatientStatus.ACTIVE;
        }
    }
}

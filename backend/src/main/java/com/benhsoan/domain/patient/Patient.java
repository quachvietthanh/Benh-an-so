package com.benhsoan.domain.patient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.enums.PatientStatus;
import com.benhsoan.domain.patient.exception.PatientAlreadyMergedException;
import com.benhsoan.domain.patient.exception.PatientConsentRequiredException;
import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Patient {

    private UUID id;

    private String patientCode;

    private String fullName;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String phone;

    private String email;

    private String address;

    private String identityNumber;

    private String insuranceNumber;

    private BloodType bloodType;

    private String emergencyContact;

    private String emergencyRelationship;

    private String emergencyPhone;

    // Guardian fields (NCL-02-CN-008 / QTN-44)
    private String guardianName;

    private String guardianRelationship;

    private String guardianPhone;

    private String guardianIdentityNumber;

    private UUID guardianUserId;

    private String consentSignerName;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    private UUID userId;

    private UUID createdBy;

    // Consent fields for personal data protection (NCL-15-CN-001 / QTN-24)
    private boolean consentAgreed;

    private Instant consentAgreedAt;

    private String consentVersion;

    private boolean consentWithdrawn;

    private Instant consentWithdrawnAt;

    private String consentWithdrawnReason;

    private boolean nonMedicalUseRestricted;

    // Merge fields (NCL-02-CN-006 / QTN-33)
    private PatientStatus status;

    private UUID mergedIntoPatientId;

    private Instant mergedAt;

    private UUID mergedBy;

    private String mergeReason;

    private Patient(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone,
            String guardianName,
            String guardianRelationship,
            String guardianPhone,
            String guardianIdentityNumber,
            UUID guardianUserId,
            String consentSignerName,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            UUID userId,
            UUID createdBy,
            boolean consentAgreed,
            Instant consentAgreedAt,
            String consentVersion,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted,
            PatientStatus status,
            UUID mergedIntoPatientId,
            Instant mergedAt,
            UUID mergedBy,
            String mergeReason
    ) {

        this.id = Objects.requireNonNull(id);

        this.patientCode = Guard.require(patientCode, "Patient code");
        this.fullName = Guard.require(fullName, "Full name");
        this.dateOfBirth = Guard.require(dateOfBirth, "Date of birth");
        this.gender = Guard.require(gender, "Gender");

        this.phone = phone;
        this.email = email;
        this.address = address;

        this.identityNumber = identityNumber;
        this.insuranceNumber = insuranceNumber;

        this.bloodType =
                bloodType == null
                        ? BloodType.UNKNOWN
                        : bloodType;

        this.emergencyContact = emergencyContact;
        this.emergencyRelationship = emergencyRelationship;
        this.emergencyPhone = emergencyPhone;

        this.guardianName = guardianName;
        this.guardianRelationship = guardianRelationship;
        this.guardianPhone = guardianPhone;
        this.guardianIdentityNumber = guardianIdentityNumber;
        this.guardianUserId = guardianUserId;
        this.consentSignerName = consentSignerName != null && !consentSignerName.isBlank()
                ? consentSignerName
                : (PatientMinorPolicy.isMinor(dateOfBirth) ? guardianName : fullName);

        this.active = active;

        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.createdBy = Objects.requireNonNull(createdBy);

        this.consentAgreed = consentAgreed;
        this.consentAgreedAt = consentAgreedAt;
        this.consentVersion = consentVersion;
        this.consentWithdrawn = consentWithdrawn;
        this.consentWithdrawnAt = consentWithdrawnAt;
        this.consentWithdrawnReason = consentWithdrawnReason;
        this.nonMedicalUseRestricted = nonMedicalUseRestricted;
        this.status = status != null ? status : (active ? PatientStatus.ACTIVE : PatientStatus.INACTIVE);
        this.mergedIntoPatientId = mergedIntoPatientId;
        this.mergedAt = mergedAt;
        this.mergedBy = mergedBy;
        this.mergeReason = mergeReason;
    }

    public static Patient create(
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone,
            String guardianName,
            String guardianRelationship,
            String guardianPhone,
            String guardianIdentityNumber,
            UUID guardianUserId,
            String consentSignerName,
            boolean consentAgreed,
            String consentVersion,
            UUID createdBy
    ) {
        if (!consentAgreed) {
            throw new PatientConsentRequiredException();
        }

        boolean isMinor = PatientMinorPolicy.isMinor(dateOfBirth);
        if (isMinor) {
            if (guardianName == null || guardianName.isBlank()) {
                throw new com.benhsoan.domain.shared.exception.ValidationException("guardianName", "Hồ sơ bệnh nhân dưới 18 tuổi bắt buộc phải khai báo người giám hộ (QTN-44).");
            }
            if (guardianRelationship == null || guardianRelationship.isBlank()) {
                throw new com.benhsoan.domain.shared.exception.ValidationException("guardianRelationship", "Mối quan hệ với người giám hộ không được để trống.");
            }
            if (guardianPhone == null || guardianPhone.isBlank()) {
                throw new com.benhsoan.domain.shared.exception.ValidationException("guardianPhone", "Số điện thoại người giám hộ không được để trống.");
            }
            if (consentSignerName != null && !consentSignerName.isBlank()
                    && !consentSignerName.trim().equalsIgnoreCase(guardianName.trim())) {
                throw new com.benhsoan.domain.shared.exception.ValidationException(
                        "consentSignerName",
                        "Đối với bệnh nhân chưa thành niên, người ký phiếu đồng ý bắt buộc phải là người giám hộ (QTN-44)."
                );
            }
        }

        String resolvedConsentSigner = isMinor
                ? guardianName
                : (consentSignerName != null && !consentSignerName.isBlank() ? consentSignerName : (guardianName != null && !guardianName.isBlank() ? guardianName : fullName));

        Instant now = Instant.now();

        return new Patient(
                UUID.randomUUID(),
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                emergencyRelationship,
                emergencyPhone,
                guardianName,
                guardianRelationship,
                guardianPhone,
                guardianIdentityNumber,
                guardianUserId,
                resolvedConsentSigner,
                true,
                now,
                now,
                null,
                createdBy,
                true,
                now,
                PatientConsentVersion.resolveForNewConsent(consentVersion),
                false,
                null,
                null,
                false,
                PatientStatus.ACTIVE,
                null,
                null,
                null,
                null
        );
    }

    public static Patient create(
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone,
            boolean consentAgreed,
            String consentVersion,
            UUID createdBy
    ) {
        return create(
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                emergencyRelationship,
                emergencyPhone,
                null,
                null,
                null,
                null,
                null,
                null,
                consentAgreed,
                consentVersion,
                createdBy
        );
    }

    public static Patient create(
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyPhone,
            boolean consentAgreed,
            String consentVersion,
            UUID createdBy
    ) {
        return create(
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                null,
                emergencyPhone,
                null,
                null,
                null,
                null,
                null,
                null,
                consentAgreed,
                consentVersion,
                createdBy
        );
    }

    public int getAge() {
        return PatientMinorPolicy.calculateAge(this.dateOfBirth);
    }

    public boolean isMinor() {
        return PatientMinorPolicy.isMinor(this.dateOfBirth);
    }

    public boolean isMinor(LocalDate asOfDate) {
        return PatientMinorPolicy.isMinor(this.dateOfBirth, asOfDate);
    }

    public boolean requiresAdultTransition() {
        return PatientMinorPolicy.requiresAdultTransition(this.dateOfBirth, this.guardianName);
    }

    public boolean requiresAdultTransition(LocalDate asOfDate) {
        return PatientMinorPolicy.requiresAdultTransition(this.dateOfBirth, this.guardianName, asOfDate);
    }

    public void updateProfile(
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone,
            String guardianName,
            String guardianRelationship,
            String guardianPhone,
            String guardianIdentityNumber,
            UUID guardianUserId,
            String consentSignerName
    ) {

        this.fullName = Guard.require(fullName, "Full name");
        this.dateOfBirth = Guard.require(dateOfBirth, "Date of birth");
        this.gender = Guard.require(gender, "Gender");

        boolean isMinor = PatientMinorPolicy.isMinor(dateOfBirth);
        if (isMinor) {
            if (guardianName == null || guardianName.isBlank()) {
                throw new com.benhsoan.domain.shared.exception.ValidationException(
                        "guardianName",
                        "Hồ sơ bệnh nhân dưới 18 tuổi bắt buộc phải khai báo người giám hộ (QTN-44)."
                );
            }
            if (guardianRelationship == null || guardianRelationship.isBlank()) {
                throw new com.benhsoan.domain.shared.exception.ValidationException(
                        "guardianRelationship",
                        "Mối quan hệ với người giám hộ không được để trống."
                );
            }
            if (guardianPhone == null || guardianPhone.isBlank()) {
                throw new com.benhsoan.domain.shared.exception.ValidationException(
                        "guardianPhone",
                        "Số điện thoại người giám hộ không được để trống."
                );
            }
            if (consentSignerName != null && !consentSignerName.isBlank()
                    && !consentSignerName.trim().equalsIgnoreCase(guardianName.trim())) {
                throw new com.benhsoan.domain.shared.exception.ValidationException(
                        "consentSignerName",
                        "Đối với bệnh nhân chưa thành niên, người ký phiếu đồng ý bắt buộc phải là người giám hộ (QTN-44)."
                );
            }
        }

        this.phone = phone;
        this.email = email;
        this.address = address;

        this.identityNumber = identityNumber;
        this.insuranceNumber = insuranceNumber;

        this.bloodType =
                bloodType == null
                        ? BloodType.UNKNOWN
                        : bloodType;

        this.emergencyContact = emergencyContact;
        this.emergencyRelationship = emergencyRelationship;
        this.emergencyPhone = emergencyPhone;

        this.guardianName = guardianName;
        this.guardianRelationship = guardianRelationship;
        this.guardianPhone = guardianPhone;
        this.guardianIdentityNumber = guardianIdentityNumber;
        this.guardianUserId = guardianUserId;
        this.consentSignerName = isMinor
                ? guardianName
                : (consentSignerName != null && !consentSignerName.isBlank()
                        ? consentSignerName
                        : (guardianName != null && !guardianName.isBlank() ? guardianName : this.fullName));

        this.updatedAt = Instant.now();
    }

    public void updateProfile(
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone
    ) {
        updateProfile(
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                emergencyRelationship,
                emergencyPhone,
                this.guardianName,
                this.guardianRelationship,
                this.guardianPhone,
                this.guardianIdentityNumber,
                this.guardianUserId,
                this.consentSignerName
        );
    }

    public void updateProfile(
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyPhone
    ) {
        updateProfile(
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                this.emergencyRelationship,
                emergencyPhone,
                this.guardianName,
                this.guardianRelationship,
                this.guardianPhone,
                this.guardianIdentityNumber,
                this.guardianUserId,
                this.consentSignerName
        );
    }

    public void transitionToAdult(String newConsentVersion, Instant agreedAt) {
        if (PatientMinorPolicy.isMinor(this.dateOfBirth)) {
            throw new com.benhsoan.domain.shared.exception.ValidationException(
                    "Bệnh nhân chưa đủ 18 tuổi, không thể chuyển sang tự chịu trách nhiệm."
            );
        }
        this.guardianName = null;
        this.guardianRelationship = null;
        this.guardianPhone = null;
        this.guardianIdentityNumber = null;
        this.guardianUserId = null;
        this.consentSignerName = this.fullName;
        renewConsent(newConsentVersion, agreedAt);
    }

    public void withdrawConsent(String reason, Instant withdrawnAt) {
        this.consentWithdrawn = true;
        if (this.consentWithdrawnAt == null || withdrawnAt != null) {
            this.consentWithdrawnAt = withdrawnAt != null ? withdrawnAt : Instant.now();
        }
        this.consentWithdrawnReason = reason;
        this.nonMedicalUseRestricted = true;
        this.updatedAt = Instant.now();
    }

    public void renewConsent(String version, Instant agreedAt) {
        this.consentAgreed = true;
        this.consentAgreedAt = agreedAt != null ? agreedAt : Instant.now();
        this.consentVersion = PatientConsentVersion.requireSupported(version);
        this.consentWithdrawn = false;
        this.consentWithdrawnAt = null;
        this.consentWithdrawnReason = null;
        this.nonMedicalUseRestricted = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void linkUser(UUID userId) {
        this.userId = java.util.Objects.requireNonNull(userId, "User id");
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public boolean isMerged() {
        return this.status == PatientStatus.MERGED;
    }

    public void markAsMerged(UUID targetPatientId, UUID mergedBy, String mergeReason) {
        markAsMerged(targetPatientId, mergedBy, mergeReason, Instant.now());
    }

    public void markAsMerged(UUID targetPatientId, UUID mergedBy, String mergeReason, Instant mergedAt) {
        if (this.isMerged()) {
            throw new PatientAlreadyMergedException(this.id, this.mergedIntoPatientId);
        }
        this.status = PatientStatus.MERGED;
        this.active = false;
        this.mergedIntoPatientId = Objects.requireNonNull(targetPatientId, "Target patient ID cannot be null");
        this.mergedBy = mergedBy;
        this.mergeReason = mergeReason;
        this.mergedAt = mergedAt != null ? mergedAt : Instant.now();
        this.updatedAt = this.mergedAt;
    }

    public void setIdForTest(UUID id) {
        this.id = id;
    }

    public void validateCanBeUpdated() {
        if (this.isMerged()) {
            throw new PatientAlreadyMergedException(this.id, this.mergedIntoPatientId);
        }
    }

    public static Patient restore(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone,
            String guardianName,
            String guardianRelationship,
            String guardianPhone,
            String guardianIdentityNumber,
            UUID guardianUserId,
            String consentSignerName,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            UUID userId,
            UUID createdBy,
            boolean consentAgreed,
            Instant consentAgreedAt,
            String consentVersion,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted,
            PatientStatus status,
            UUID mergedIntoPatientId,
            Instant mergedAt,
            UUID mergedBy,
            String mergeReason
    ) {
        return new Patient(
                id,
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                emergencyRelationship,
                emergencyPhone,
                guardianName,
                guardianRelationship,
                guardianPhone,
                guardianIdentityNumber,
                guardianUserId,
                consentSignerName != null && !consentSignerName.isBlank()
                        ? consentSignerName
                        : (PatientMinorPolicy.isMinor(dateOfBirth) ? guardianName : fullName),
                active,
                createdAt,
                updatedAt,
                userId,
                createdBy,
                consentAgreed,
                consentAgreedAt,
                consentVersion,
                consentWithdrawn,
                consentWithdrawnAt,
                consentWithdrawnReason,
                nonMedicalUseRestricted,
                status,
                mergedIntoPatientId,
                mergedAt,
                mergedBy,
                mergeReason
        );
    }

    public static Patient restore(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone,
            String guardianName,
            String guardianRelationship,
            String guardianPhone,
            String guardianIdentityNumber,
            UUID guardianUserId,
            String consentSignerName,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            UUID userId,
            UUID createdBy,
            boolean consentAgreed,
            Instant consentAgreedAt,
            String consentVersion,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted
    ) {

        return restore(
                id,
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                emergencyRelationship,
                emergencyPhone,
                guardianName,
                guardianRelationship,
                guardianPhone,
                guardianIdentityNumber,
                guardianUserId,
                consentSignerName,
                active,
                createdAt,
                updatedAt,
                userId,
                createdBy,
                consentAgreed,
                consentAgreedAt,
                consentVersion,
                consentWithdrawn,
                consentWithdrawnAt,
                consentWithdrawnReason,
                nonMedicalUseRestricted,
                active ? PatientStatus.ACTIVE : PatientStatus.INACTIVE,
                null,
                null,
                null,
                null
        );
    }

    public static Patient restore(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            UUID userId,
            UUID createdBy,
            boolean consentAgreed,
            Instant consentAgreedAt,
            String consentVersion,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted
    ) {
        return restore(
                id,
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                emergencyRelationship,
                emergencyPhone,
                null,
                null,
                null,
                null,
                null,
                null,
                active,
                createdAt,
                updatedAt,
                userId,
                createdBy,
                consentAgreed,
                consentAgreedAt,
                consentVersion,
                consentWithdrawn,
                consentWithdrawnAt,
                consentWithdrawnReason,
                nonMedicalUseRestricted
        );
    }

    public static Patient restore(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyPhone,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            UUID userId,
            UUID createdBy,
            boolean consentAgreed,
            Instant consentAgreedAt,
            String consentVersion,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted
    ) {

        return restore(
                id,
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                null,
                emergencyPhone,
                active,
                createdAt,
                updatedAt,
                userId,
                createdBy,
                consentAgreed,
                consentAgreedAt,
                consentVersion,
                consentWithdrawn,
                consentWithdrawnAt,
                consentWithdrawnReason,
                nonMedicalUseRestricted
        );
    }

    public static Patient restore(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyPhone,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            UUID userId,
            UUID createdBy
    ) {
        return restore(
                id,
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                null,
                emergencyPhone,
                active,
                createdAt,
                updatedAt,
                userId,
                createdBy,
                false,
                null,
                null,
                false,
                null,
                null,
                false
        );
    }
}

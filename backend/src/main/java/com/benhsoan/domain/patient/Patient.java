package com.benhsoan.domain.patient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
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

    private String emergencyPhone;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    private UUID userId;

    private UUID createdBy;

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
            String emergencyPhone,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            UUID userId,
            UUID createdBy
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
        this.emergencyPhone = emergencyPhone;

        this.active = active;

        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.createdBy = Objects.requireNonNull(createdBy);
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
            UUID createdBy
    ) {

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
                emergencyPhone,
                true,
                Instant.now(),
                Instant.now(),
                null,
                createdBy
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
        this.emergencyPhone = emergencyPhone;

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
                emergencyPhone,
                active,
                createdAt,
                updatedAt,
                userId,
                createdBy
        );
    }
}

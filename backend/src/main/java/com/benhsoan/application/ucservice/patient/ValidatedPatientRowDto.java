package com.benhsoan.application.ucservice.patient;

import java.time.LocalDate;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ValidatedPatientRowDto {
    private int rowNumber;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String phone;
    private String identityNumber;
    private String insuranceNumber;
    private String address;
    private String email;
    private BloodType bloodType;
    private String emergencyContact;
    private String emergencyRelationship;
    private String emergencyPhone;
    private String guardianName;
    private String guardianRelationship;
    private String guardianPhone;
    private String rawData;
}

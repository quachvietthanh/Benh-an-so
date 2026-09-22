package com.benhsoan.port.dto.spreadsheet;

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
public class RawPatientRowDto {
    private int rowNumber;
    private String fullName;
    private String dateOfBirth;
    private String gender;
    private String phone;
    private String identityNumber;
    private String insuranceNumber;
    private String address;
    private String email;
    private String bloodType;
    private String emergencyContact;
    private String emergencyRelationship;
    private String emergencyPhone;
    private String guardianName;
    private String guardianRelationship;
    private String guardianPhone;

    public boolean isEmpty() {
        return isBlank(fullName)
                && isBlank(dateOfBirth)
                && isBlank(gender)
                && isBlank(phone)
                && isBlank(identityNumber)
                && isBlank(insuranceNumber)
                && isBlank(address)
                && isBlank(email)
                && isBlank(emergencyContact)
                && isBlank(guardianName);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

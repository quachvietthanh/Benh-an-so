package com.benhsoan.adapter.inbound.rest.request.patient;

import java.time.LocalDate;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePatientRequest(

        @NotBlank
        String fullName,

        @NotNull
        LocalDate dateOfBirth,

        @NotNull
        Gender gender,

        String phone,

        String email,

        String address,

        String identityNumber,

        String insuranceNumber,

        BloodType bloodType,

        String emergencyContact,

        @Size(max = 50, message = "Mối quan hệ không được vượt quá 50 ký tự.")
        String emergencyRelationship,

        @Pattern(regexp = "^(?:(0|\\+84)(3|5|7|8|9)[0-9]{8}|[0-9]{2}\\*{6}[0-9]{2})?$", message = "Số điện thoại không đúng định dạng.")
        String emergencyPhone,

        boolean active,

        Boolean consentAgreed,

        Boolean consentWithdrawn,

        String consentWithdrawnReason,

        String consentVersion,

        String guardianName,

        @Size(max = 50, message = "Mối quan hệ với người giám hộ không được vượt quá 50 ký tự.")
        String guardianRelationship,

        @Pattern(regexp = "^(?:(0|\\+84)(3|5|7|8|9)[0-9]{8}|[0-9]{2}\\*{6}[0-9]{2})?$", message = "Số điện thoại người giám hộ không đúng định dạng.")
        String guardianPhone,

        String guardianIdentityNumber,

        String consentSignerName,

        Boolean transitionToAdult

) {
}
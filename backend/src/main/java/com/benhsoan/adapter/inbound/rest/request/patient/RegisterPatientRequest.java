package com.benhsoan.adapter.inbound.rest.request.patient;

import java.time.LocalDate;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterPatientRequest(

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

        String emergencyPhone,

        @NotNull(message = "Sự đồng ý xử lý dữ liệu cá nhân là bắt buộc (QTN-24).")
        @AssertTrue(message = "Phải ghi nhận sự đồng ý của người bệnh trước khi lập hồ sơ mới (QTN-24).")
        Boolean consentAgreed,

        String consentVersion

) {
}
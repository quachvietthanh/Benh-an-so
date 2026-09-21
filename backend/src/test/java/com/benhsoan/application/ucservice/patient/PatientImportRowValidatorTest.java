package com.benhsoan.application.ucservice.patient;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.port.dto.spreadsheet.RawPatientRowDto;

class PatientImportRowValidatorTest {

    private final PatientImportRowValidator validator = new PatientImportRowValidator();

    @Test
    @DisplayName("Should validate valid adult row successfully")
    void shouldValidateValidAdultRow() {
        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Trần Thị Hoa")
                .dateOfBirth("12/03/1995")
                .gender("Nữ")
                .phone("0987654321")
                .identityNumber("001195000123")
                .bloodType("B")
                .address("Hà Nội")
                .build();

        PatientImportRowValidator.RowValidationResult result = validator.validate(raw);

        assertThat(result.isValid()).isTrue();
        assertThat(result.error()).isNull();
        assertThat(result.validRow()).isNotNull();
        assertThat(result.validRow().getFullName()).isEqualTo("Trần Thị Hoa");
        assertThat(result.validRow().getDateOfBirth()).isEqualTo(LocalDate.of(1995, 3, 12));
        assertThat(result.validRow().getGender()).isEqualTo(Gender.FEMALE);
        assertThat(result.validRow().getBloodType()).isEqualTo(BloodType.B_POSITIVE);
    }

    @Test
    @DisplayName("Should validate valid minor row with required guardian successfully (QTN-44)")
    void shouldValidateValidMinorRow() {
        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(3)
                .fullName("Nguyễn Văn Tí")
                .dateOfBirth("01/01/2018")
                .gender("Nam")
                .guardianName("Nguyễn Văn Bố")
                .guardianRelationship("Bố")
                .guardianPhone("0912345678")
                .build();

        PatientImportRowValidator.RowValidationResult result = validator.validate(raw);

        assertThat(result.isValid()).isTrue();
        assertThat(result.validRow().getGuardianName()).isEqualTo("Nguyễn Văn Bố");
    }

    @Test
    @DisplayName("Should fail when full name is missing")
    void shouldFailWhenFullNameIsMissing() {
        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(4)
                .fullName("")
                .dateOfBirth("12/03/1995")
                .gender("Nam")
                .build();

        PatientImportRowValidator.RowValidationResult result = validator.validate(raw);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error()).isNotNull();
        assertThat(result.error().getErrorField()).isEqualTo("Họ và tên");
    }

    @Test
    @DisplayName("Should fail when date of birth is in the future")
    void shouldFailWhenDateOfBirthIsInFuture() {
        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(5)
                .fullName("Lê Văn Tương Lai")
                .dateOfBirth("12/03/2099")
                .gender("Nam")
                .build();

        PatientImportRowValidator.RowValidationResult result = validator.validate(raw);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error().getErrorMessage()).contains("không thể ở tương lai");
    }

    @Test
    @DisplayName("Should fail when date of birth has invalid format")
    void shouldFailWhenDateOfBirthIsInvalid() {
        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(6)
                .fullName("Lê Văn Sai Ngày")
                .dateOfBirth("31-02-1990")
                .gender("Nam")
                .build();

        PatientImportRowValidator.RowValidationResult result = validator.validate(raw);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error().getErrorField()).isEqualTo("Ngày sinh");
    }

    @Test
    @DisplayName("Should fail when minor lacks guardian information (QTN-44)")
    void shouldFailWhenMinorLacksGuardian() {
        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(7)
                .fullName("Em Bé Chưa Có Giám Hộ")
                .dateOfBirth("15/06/2015")
                .gender("Nữ")
                .build();

        PatientImportRowValidator.RowValidationResult result = validator.validate(raw);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error().getErrorField()).isEqualTo("Tên người giám hộ");
        assertThat(result.error().getErrorMessage()).contains("QTN-44");
    }

    @Test
    @DisplayName("Should fail when phone format is invalid")
    void shouldFailWhenPhoneIsInvalid() {
        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(8)
                .fullName("Phạm Văn Điện Thoại Sai")
                .dateOfBirth("10/10/1990")
                .gender("Nam")
                .phone("12345")
                .build();

        PatientImportRowValidator.RowValidationResult result = validator.validate(raw);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error().getErrorField()).isEqualTo("Số điện thoại");
    }

    @Test
    @DisplayName("Should fail when CCCD/CMND format is invalid (not 9 or 12 digits)")
    void shouldFailWhenIdentityNumberIsInvalid() {
        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(9)
                .fullName("Đỗ Văn Căn Cước")
                .dateOfBirth("10/10/1990")
                .gender("Nam")
                .identityNumber("1234567")
                .build();

        PatientImportRowValidator.RowValidationResult result = validator.validate(raw);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error().getErrorField()).isEqualTo("Số CCCD/CMND");
    }
}

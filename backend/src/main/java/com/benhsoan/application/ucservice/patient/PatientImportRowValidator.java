package com.benhsoan.application.ucservice.patient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientImportRowError;
import com.benhsoan.domain.patient.PatientMinorPolicy;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.infrastructure.spreadsheet.RawPatientRowDto;

@Component
public class PatientImportRowValidator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");
    private static final Pattern IDENTITY_PATTERN = Pattern.compile("^(\\d{9}|\\d{12})$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/M/yyyy")
    };

    public record RowValidationResult(ValidatedPatientRowDto validRow, PatientImportRowError error) {
        public boolean isValid() {
            return error == null && validRow != null;
        }
    }

    public RowValidationResult validate(RawPatientRowDto raw) {
        int rowNum = raw.getRowNumber();
        String rawSummary = raw.toString();

        // 1. Validate Full Name
        String fullName = raw.getFullName();
        if (fullName == null || fullName.isBlank()) {
            return new RowValidationResult(null, PatientImportRowError.create(
                    rowNum, "Họ và tên", "Họ và tên không được để trống.", rawSummary));
        }
        fullName = fullName.trim();
        if (fullName.length() > 100) {
            return new RowValidationResult(null, PatientImportRowError.create(
                    rowNum, "Họ và tên", "Họ và tên không được vượt quá 100 ký tự.", rawSummary));
        }

        // 2. Validate Date of Birth
        String dobStr = raw.getDateOfBirth();
        if (dobStr == null || dobStr.isBlank()) {
            return new RowValidationResult(null, PatientImportRowError.create(
                    rowNum, "Ngày sinh", "Ngày sinh không được để trống.", rawSummary));
        }
        LocalDate dateOfBirth = parseDate(dobStr.trim());
        if (dateOfBirth == null) {
            return new RowValidationResult(null, PatientImportRowError.create(
                    rowNum, "Ngày sinh", "Ngày sinh không đúng định dạng (hợp lệ: dd/MM/yyyy hoặc yyyy-MM-dd).", rawSummary));
        }
        if (dateOfBirth.isAfter(LocalDate.now())) {
            return new RowValidationResult(null, PatientImportRowError.create(
                    rowNum, "Ngày sinh", "Ngày sinh không thể ở tương lai.", rawSummary));
        }

        // 3. Validate Gender
        String genderStr = raw.getGender();
        if (genderStr == null || genderStr.isBlank()) {
            return new RowValidationResult(null, PatientImportRowError.create(
                    rowNum, "Giới tính", "Giới tính không được để trống.", rawSummary));
        }
        Gender gender = parseGender(genderStr.trim());
        if (gender == null) {
            return new RowValidationResult(null, PatientImportRowError.create(
                    rowNum, "Giới tính", "Giới tính không hợp lệ (chỉ chấp nhận Nam/Nữ/Khác).", rawSummary));
        }

        // 4. Validate Phone
        String phone = normalizePhone(raw.getPhone());
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            return new RowValidationResult(null, PatientImportRowError.create(
                    rowNum, "Số điện thoại", "Số điện thoại không đúng định dạng số di động Việt Nam.", rawSummary));
        }

        // 5. Validate Identity Number (CCCD/CMND)
        String identityNumber = raw.getIdentityNumber() != null ? raw.getIdentityNumber().trim() : null;
        if (identityNumber != null && !identityNumber.isEmpty()) {
            if (!IDENTITY_PATTERN.matcher(identityNumber).matches()) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "Số CCCD/CMND", "Số CCCD/CMND phải gồm 9 hoặc 12 chữ số.", rawSummary));
            }
        } else {
            identityNumber = null;
        }

        // 6. Validate Email
        String email = raw.getEmail() != null ? raw.getEmail().trim() : null;
        if (email != null && !email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            return new RowValidationResult(null, PatientImportRowError.create(
                    rowNum, "Email", "Email không đúng định dạng.", rawSummary));
        }

        // 7. Validate Blood Type
        BloodType bloodType = BloodType.UNKNOWN;
        if (raw.getBloodType() != null && !raw.getBloodType().isBlank()) {
            bloodType = parseBloodType(raw.getBloodType().trim());
        }

        // 8. Validate Guardian for Minors (<18 years old - QTN-44)
        boolean isMinor = PatientMinorPolicy.isMinor(dateOfBirth);
        String guardianName = raw.getGuardianName() != null ? raw.getGuardianName().trim() : null;
        String guardianRelationship = raw.getGuardianRelationship() != null ? raw.getGuardianRelationship().trim() : null;
        String guardianPhone = normalizePhone(raw.getGuardianPhone());

        if (isMinor) {
            if (guardianName == null || guardianName.isEmpty()) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "Tên người giám hộ", "Bệnh nhân dưới 18 tuổi bắt buộc phải khai báo tên người giám hộ (QTN-44).", rawSummary));
            }
            if (guardianRelationship == null || guardianRelationship.isEmpty()) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "Quan hệ người giám hộ", "Mối quan hệ với người giám hộ không được để trống (QTN-44).", rawSummary));
            }
            if (guardianPhone == null || guardianPhone.isEmpty()) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "SĐT người giám hộ", "Số điện thoại người giám hộ không được để trống (QTN-44).", rawSummary));
            }
            if (!PHONE_PATTERN.matcher(guardianPhone).matches()) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "SĐT người giám hộ", "Số điện thoại người giám hộ không đúng định dạng.", rawSummary));
            }
        } else {
            if (guardianPhone != null && !PHONE_PATTERN.matcher(guardianPhone).matches()) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "SĐT người giám hộ", "Số điện thoại người giám hộ không đúng định dạng.", rawSummary));
            }
        }

        // 9. Validate Emergency Contact
        String emergencyContact = raw.getEmergencyContact() != null ? raw.getEmergencyContact().trim() : null;
        String emergencyRelationship = raw.getEmergencyRelationship() != null ? raw.getEmergencyRelationship().trim() : null;
        String emergencyPhone = normalizePhone(raw.getEmergencyPhone());

        boolean hasContact = emergencyContact != null && !emergencyContact.isEmpty();
        boolean hasRel = emergencyRelationship != null && !emergencyRelationship.isEmpty();
        boolean hasPhone = emergencyPhone != null && !emergencyPhone.isEmpty();

        if (hasContact || hasRel || hasPhone) {
            if (!hasContact) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "Người liên hệ khẩn cấp", "Họ tên người liên hệ khẩn cấp không được để trống.", rawSummary));
            }
            if (!hasRel) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "Quan hệ người liên hệ", "Mối quan hệ với người liên hệ khẩn cấp không được để trống.", rawSummary));
            }
            if (!hasPhone) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "SĐT người liên hệ", "Số điện thoại người liên hệ khẩn cấp không được để trống.", rawSummary));
            }
            if (!PHONE_PATTERN.matcher(emergencyPhone).matches()) {
                return new RowValidationResult(null, PatientImportRowError.create(
                        rowNum, "SĐT người liên hệ", "Số điện thoại người liên hệ khẩn cấp không đúng định dạng.", rawSummary));
            }
        }

        ValidatedPatientRowDto validRow = ValidatedPatientRowDto.builder()
                .rowNumber(rowNum)
                .fullName(fullName)
                .dateOfBirth(dateOfBirth)
                .gender(gender)
                .phone(phone)
                .identityNumber(identityNumber)
                .insuranceNumber(raw.getInsuranceNumber() != null ? raw.getInsuranceNumber().trim() : null)
                .address(raw.getAddress() != null ? raw.getAddress().trim() : null)
                .email(email)
                .bloodType(bloodType)
                .emergencyContact(emergencyContact)
                .emergencyRelationship(emergencyRelationship)
                .emergencyPhone(emergencyPhone)
                .guardianName(guardianName)
                .guardianRelationship(guardianRelationship)
                .guardianPhone(guardianPhone)
                .rawData(rawSummary)
                .build();

        return new RowValidationResult(validRow, null);
    }

    private LocalDate parseDate(String val) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(val, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private Gender parseGender(String val) {
        String lower = val.toLowerCase();
        if (lower.equals("nam") || lower.equals("male") || lower.equals("m")) {
            return Gender.MALE;
        }
        if (lower.equals("nữ") || lower.equals("nu") || lower.equals("female") || lower.equals("f")) {
            return Gender.FEMALE;
        }
        if (lower.equals("khác") || lower.equals("khac") || lower.equals("other") || lower.equals("o")) {
            return Gender.OTHER;
        }
        return null;
    }

    private BloodType parseBloodType(String val) {
        if (val == null || val.isBlank()) {
            return BloodType.UNKNOWN;
        }
        String clean = val.trim().toUpperCase().replace(" ", "_");
        return switch (clean) {
            case "A", "A+", "A_POS", "A_POSITIVE" -> BloodType.A_POSITIVE;
            case "A-", "A_NEG", "A_NEGATIVE" -> BloodType.A_NEGATIVE;
            case "B", "B+", "B_POS", "B_POSITIVE" -> BloodType.B_POSITIVE;
            case "B-", "B_NEG", "B_NEGATIVE" -> BloodType.B_NEGATIVE;
            case "AB", "AB+", "AB_POS", "AB_POSITIVE" -> BloodType.AB_POSITIVE;
            case "AB-", "AB_NEG", "AB_NEGATIVE" -> BloodType.AB_NEGATIVE;
            case "O", "O+", "O_POS", "O_POSITIVE" -> BloodType.O_POSITIVE;
            case "O-", "O_NEG", "O_NEGATIVE" -> BloodType.O_NEGATIVE;
            default -> BloodType.UNKNOWN;
        };
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim().replaceAll("\\s+", "");
        return trimmed.startsWith("+84") ? "0" + trimmed.substring(3) : trimmed;
    }
}

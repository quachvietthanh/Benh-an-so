package com.benhsoan.application.ucservice.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientMinorPolicy;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.enums.ImportStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.spreadsheet.ExcelPatientSheetParser;
import com.benhsoan.port.dto.command.patient.ImportPatientsCommand;
import com.benhsoan.port.dto.command.patient.SearchPatientCommand;
import com.benhsoan.port.dto.result.patient.PatientImportResult;
import com.benhsoan.port.dto.spreadsheet.RawPatientRowDto;
import com.benhsoan.port.outbound.generator.PatientCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientImportLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.spreadsheet.PatientSpreadsheetParserPort;

@DisplayName("Patient Import Edge Cases & Gap Verification Tests (NCL-02-CN-010)")
class PatientImportEdgeCasesTest {

    @Test
    @DisplayName("P1-01: Should accept date of birth as today in Vietnam timezone (currentDate)")
    void shouldAcceptTodayDateOfBirthInVietnamTimezone() {
        PatientImportRowValidator validator = new PatientImportRowValidator();
        LocalDate today = PatientMinorPolicy.currentDate();

        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Trần Bé Sơ Sinh")
                .dateOfBirth(today.toString())
                .gender("Nữ")
                .guardianName("Trần Văn Cha")
                .guardianRelationship("Bố")
                .guardianPhone("0912345678")
                .build();

        PatientImportRowValidator.RowValidationResult result = validator.validate(raw);

        assertThat(result.isValid()).isTrue();
        assertThat(result.validRow().getDateOfBirth()).isEqualTo(today);
    }

    @Test
    @DisplayName("P2-01: Should safely serialize AuditLog JSON even if fileName contains double quotes and special chars")
    void shouldSafelySerializeAuditLogJsonWithQuotesInFileName() {
        PatientSpreadsheetParserPort parser = mock(PatientSpreadsheetParserPort.class);
        PatientImportRowValidator validator = mock(PatientImportRowValidator.class);
        PatientImportDuplicateDetector detector = mock(PatientImportDuplicateDetector.class);
        PatientRepository patientRepository = mock(PatientRepository.class);
        PatientImportLogRepository logRepo = mock(PatientImportLogRepository.class);
        PatientChangeLogRepository changeRepo = mock(PatientChangeLogRepository.class);
        PatientCodeGenerator codeGen = mock(PatientCodeGenerator.class);
        CurrentUserPort userPort = mock(CurrentUserPort.class);
        AuditLogRepository auditRepo = mock(AuditLogRepository.class);
        PatientChangeDetailBuilder detailBuilder = mock(PatientChangeDetailBuilder.class);
        ObjectMapper objectMapper = new ObjectMapper();

        UUID actorId = UUID.randomUUID();
        when(userPort.getCurrentUserId()).thenReturn(actorId);
        when(codeGen.generate()).thenReturn("BN-TEST-001");
        when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));
        when(logRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        ImportPatientsService service = new ImportPatientsService(
                parser, validator, detector, patientRepository, logRepo,
                changeRepo, codeGen, userPort, auditRepo, detailBuilder, objectMapper
        );

        String maliciousFileName = "danh_sach_\"PK_TEST\"\\special\n.xlsx";
        RawPatientRowDto raw = RawPatientRowDto.builder().rowNumber(2).fullName("Test").build();
        when(parser.parse(any())).thenReturn(List.of(raw));

        ValidatedPatientRowDto validRow = ValidatedPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Nguyễn Văn Test")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .phone("0912345678")
                .bloodType(BloodType.O_POSITIVE)
                .build();

        when(validator.validate(any())).thenReturn(new PatientImportRowValidator.RowValidationResult(validRow, null));
        when(detector.detectDuplicates(any())).thenReturn(new PatientImportDuplicateDetector.DuplicateCheckResult(List.of(validRow), List.of()));

        ImportPatientsCommand cmd = new ImportPatientsCommand(
                new byte[]{1, 2, 3},
                maliciousFileName,
                1024L,
                true
        );

        PatientImportResult result = service.importPatients(cmd);

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepo).save(captor.capture());
        AuditLog audit = captor.getValue();

        assertThat(audit.getDetail()).isNotNull();
        // Verify detail is valid parseable JSON
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            objectMapper.readTree(audit.getDetail());
        });
        assertThat(audit.getDetail()).contains("PK_TEST");
    }

    @Test
    @DisplayName("P3-02: Should detect database duplicate for minor without phone via guardianPhone or Name+DOB")
    void shouldDetectDuplicateForMinorWithoutPhoneViaNameAndDob() {
        PatientRepository repo = mock(PatientRepository.class);
        PatientImportDuplicateDetector detector = new PatientImportDuplicateDetector(repo);

        UUID existingId = UUID.randomUUID();
        Patient existingPatient = mock(Patient.class);
        when(existingPatient.getId()).thenReturn(existingId);
        when(existingPatient.getPatientCode()).thenReturn("BN-EXISTING-01");
        when(existingPatient.getFullName()).thenReturn("Nguyễn Minh Khang");
        when(existingPatient.getDateOfBirth()).thenReturn(LocalDate.of(2018, 5, 20));
        when(existingPatient.isMerged()).thenReturn(false);

        when(repo.search(any(SearchPatientCommand.class))).thenReturn(new PageImpl<>(List.of(existingPatient)));

        ValidatedPatientRowDto incomingMinor = ValidatedPatientRowDto.builder()
                .rowNumber(5)
                .fullName("Nguyễn Minh Khang")
                .dateOfBirth(LocalDate.of(2018, 5, 20))
                .gender(Gender.MALE)
                .phone(null) // Minor without personal phone
                .guardianName("Nguyễn Văn An")
                .guardianRelationship("Bố")
                .guardianPhone("0909999999") // Different guardian phone but same name and DOB
                .build();

        PatientImportDuplicateDetector.DuplicateCheckResult checkResult =
                detector.detectDuplicates(List.of(incomingMinor));

        assertThat(checkResult.nonDuplicateRows()).isEmpty();
        assertThat(checkResult.suspectedDuplicates()).hasSize(1);
        assertThat(checkResult.suspectedDuplicates().get(0).matchedExistingPatientCode()).isEqualTo("BN-EXISTING-01");
        assertThat(checkResult.suspectedDuplicates().get(0).duplicateReason()).contains("Hồ sơ trùng khớp Họ tên và Ngày sinh");
    }

    @Test
    @DisplayName("Boundary: ExcelPatientSheetParser should reject file with more than 5000 rows")
    void shouldRejectExcelWithMoreThan5000Rows() throws Exception {
        ExcelPatientSheetParser parser = new ExcelPatientSheetParser();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Họ và tên");

            for (int i = 1; i <= 5005; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue("Bệnh nhân " + i);
                row.createCell(1).setCellValue("1990-01-01");
            }
            wb.write(out);

            byte[] bytes = out.toByteArray();
            assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(bytes)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("vượt quá giới hạn 5000 dòng");
        }
    }
}

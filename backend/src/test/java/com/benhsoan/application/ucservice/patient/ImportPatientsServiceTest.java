package com.benhsoan.application.ucservice.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientImportLog;
import com.benhsoan.domain.patient.PatientImportRowError;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.enums.ImportStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.spreadsheet.ExcelPatientSheetParser;
import com.benhsoan.port.dto.spreadsheet.RawPatientRowDto;
import com.benhsoan.port.dto.command.patient.ImportPatientsCommand;
import com.benhsoan.port.dto.result.patient.PatientImportResult;
import com.benhsoan.port.outbound.generator.PatientCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientImportLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class ImportPatientsServiceTest {

    private com.benhsoan.port.outbound.spreadsheet.PatientSpreadsheetParserPort sheetParser;
    private PatientImportRowValidator rowValidator;
    private PatientImportDuplicateDetector duplicateDetector;
    private PatientRepository patientRepository;
    private PatientImportLogRepository patientImportLogRepository;
    private PatientChangeLogRepository patientChangeLogRepository;
    private PatientCodeGenerator patientCodeGenerator;
    private CurrentUserPort currentUserPort;
    private AuditLogRepository auditLogRepository;
    private PatientChangeDetailBuilder changeDetailBuilder;

    private ImportPatientsService service;

    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sheetParser = mock(com.benhsoan.port.outbound.spreadsheet.PatientSpreadsheetParserPort.class);
        rowValidator = mock(PatientImportRowValidator.class);
        duplicateDetector = mock(PatientImportDuplicateDetector.class);
        patientRepository = mock(PatientRepository.class);
        patientImportLogRepository = mock(PatientImportLogRepository.class);
        patientChangeLogRepository = mock(PatientChangeLogRepository.class);
        patientCodeGenerator = mock(PatientCodeGenerator.class);
        currentUserPort = mock(CurrentUserPort.class);
        auditLogRepository = mock(AuditLogRepository.class);
        changeDetailBuilder = mock(PatientChangeDetailBuilder.class);

        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);

        service = new ImportPatientsService(
                sheetParser,
                rowValidator,
                duplicateDetector,
                patientRepository,
                patientImportLogRepository,
                patientChangeLogRepository,
                patientCodeGenerator,
                currentUserPort,
                auditLogRepository,
                changeDetailBuilder,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    @Test
    @DisplayName("Should throw ValidationException when file bytes are empty")
    void shouldThrowWhenFileIsEmpty() {
        ImportPatientsCommand cmd = new ImportPatientsCommand(new byte[0], "test.xlsx", 0, true);
        assertThatThrownBy(() -> service.importPatients(cmd))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Should successfully import valid rows and save logs")
    void shouldImportValidRowsSuccessfully() {
        byte[] mockBytes = new byte[]{1, 2, 3};
        ImportPatientsCommand cmd = new ImportPatientsCommand(mockBytes, "valid.xlsx", mockBytes.length, true);

        RawPatientRowDto raw = RawPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Nguyễn Văn An")
                .dateOfBirth("15/05/1988")
                .gender("Nam")
                .build();

        ValidatedPatientRowDto validRow = ValidatedPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Nguyễn Văn An")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .gender(Gender.MALE)
                .build();

        when(sheetParser.parse(any(InputStream.class))).thenReturn(List.of(raw));
        when(rowValidator.validate(raw)).thenReturn(new PatientImportRowValidator.RowValidationResult(validRow, null));
        when(duplicateDetector.detectDuplicates(List.of(validRow))).thenReturn(
                new PatientImportDuplicateDetector.DuplicateCheckResult(List.of(validRow), List.of())
        );

        when(patientCodeGenerator.generate()).thenReturn("BN-100001");

        Patient savedPatient = mock(Patient.class);
        when(savedPatient.getId()).thenReturn(UUID.randomUUID());
        when(savedPatient.getPatientCode()).thenReturn("BN-100001");
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        PatientImportLog mockSavedLog = mock(PatientImportLog.class);
        when(mockSavedLog.getId()).thenReturn(UUID.randomUUID());
        when(patientImportLogRepository.save(any(PatientImportLog.class))).thenReturn(mockSavedLog);

        PatientImportResult result = service.importPatients(cmd);

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isEqualTo(0);
        assertThat(result.createdPatientCodes()).containsExactly("BN-100001");

        verify(patientRepository).save(any(Patient.class));
        verify(patientChangeLogRepository).save(any());
        verify(patientImportLogRepository).save(any(PatientImportLog.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Should record row-level error and proceed with partial success (AC-02)")
    void shouldHandlePartialSuccess() {
        byte[] mockBytes = new byte[]{1, 2, 3};
        ImportPatientsCommand cmd = new ImportPatientsCommand(mockBytes, "partial.xlsx", mockBytes.length, true);

        RawPatientRowDto raw1 = RawPatientRowDto.builder().rowNumber(2).fullName("Nguyễn Văn A").build();
        RawPatientRowDto raw2 = RawPatientRowDto.builder().rowNumber(3).fullName("").build(); // Invalid

        ValidatedPatientRowDto validRow = ValidatedPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build();

        PatientImportRowError errorRow = PatientImportRowError.create(3, "Họ và tên", "Họ và tên không được để trống", "");

        when(sheetParser.parse(any(InputStream.class))).thenReturn(List.of(raw1, raw2));
        when(rowValidator.validate(raw1)).thenReturn(new PatientImportRowValidator.RowValidationResult(validRow, null));
        when(rowValidator.validate(raw2)).thenReturn(new PatientImportRowValidator.RowValidationResult(null, errorRow));

        when(duplicateDetector.detectDuplicates(List.of(validRow))).thenReturn(
                new PatientImportDuplicateDetector.DuplicateCheckResult(List.of(validRow), List.of())
        );

        when(patientCodeGenerator.generate()).thenReturn("BN-100002");
        Patient savedPatient = mock(Patient.class);
        when(savedPatient.getId()).thenReturn(UUID.randomUUID());
        when(savedPatient.getPatientCode()).thenReturn("BN-100002");
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        PatientImportLog mockSavedLog = mock(PatientImportLog.class);
        when(mockSavedLog.getId()).thenReturn(UUID.randomUUID());
        when(patientImportLogRepository.save(any(PatientImportLog.class))).thenReturn(mockSavedLog);

        PatientImportResult result = service.importPatients(cmd);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(3);
    }
}

package com.benhsoan.application.ucservice.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.PatientImportRowError;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.spreadsheet.ExcelPatientSheetParser;
import com.benhsoan.infrastructure.spreadsheet.RawPatientRowDto;
import com.benhsoan.port.dto.command.patient.PreviewPatientImportCommand;
import com.benhsoan.port.dto.result.patient.PatientImportPreviewResult;
import com.benhsoan.port.dto.result.patient.SuspectedDuplicateResult;

class PreviewPatientImportServiceTest {

    private ExcelPatientSheetParser sheetParser;
    private PatientImportRowValidator rowValidator;
    private PatientImportDuplicateDetector duplicateDetector;

    private PreviewPatientImportService service;

    @BeforeEach
    void setUp() {
        sheetParser = mock(ExcelPatientSheetParser.class);
        rowValidator = mock(PatientImportRowValidator.class);
        duplicateDetector = mock(PatientImportDuplicateDetector.class);

        service = new PreviewPatientImportService(sheetParser, rowValidator, duplicateDetector);
    }

    @Test
    @DisplayName("Should throw ValidationException when preview file bytes are empty")
    void shouldThrowWhenFileEmpty() {
        PreviewPatientImportCommand cmd = new PreviewPatientImportCommand(new byte[0], "empty.xlsx", 0);
        assertThatThrownBy(() -> service.preview(cmd))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Should return preview statistics with valid, error, and duplicate counts")
    void shouldReturnPreviewStats() {
        byte[] bytes = new byte[]{1, 2};
        PreviewPatientImportCommand cmd = new PreviewPatientImportCommand(bytes, "preview.xlsx", 2);

        RawPatientRowDto raw1 = RawPatientRowDto.builder().rowNumber(2).build();
        RawPatientRowDto raw2 = RawPatientRowDto.builder().rowNumber(3).build();

        ValidatedPatientRowDto valid1 = ValidatedPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build();

        PatientImportRowError err2 = PatientImportRowError.create(3, "Họ và tên", "Thiếu họ tên", "");

        when(sheetParser.parse(any(InputStream.class))).thenReturn(List.of(raw1, raw2));
        when(rowValidator.validate(raw1)).thenReturn(new PatientImportRowValidator.RowValidationResult(valid1, null));
        when(rowValidator.validate(raw2)).thenReturn(new PatientImportRowValidator.RowValidationResult(null, err2));

        when(duplicateDetector.detectDuplicates(List.of(valid1))).thenReturn(
                new PatientImportDuplicateDetector.DuplicateCheckResult(List.of(valid1), List.of())
        );

        PatientImportPreviewResult result = service.preview(cmd);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.validCount()).isEqualTo(1);
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.duplicateCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
    }
}

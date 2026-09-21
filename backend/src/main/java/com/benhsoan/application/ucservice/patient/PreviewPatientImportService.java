package com.benhsoan.application.ucservice.patient;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.PatientImportRowError;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.spreadsheet.ExcelPatientSheetParser;
import com.benhsoan.infrastructure.spreadsheet.RawPatientRowDto;
import com.benhsoan.port.dto.command.patient.PreviewPatientImportCommand;
import com.benhsoan.port.dto.result.patient.PatientImportPreviewResult;
import com.benhsoan.port.dto.result.patient.PatientImportRowErrorResult;
import com.benhsoan.port.inbound.patient.PreviewPatientImportUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreviewPatientImportService implements PreviewPatientImportUseCase {

    private final ExcelPatientSheetParser sheetParser;
    private final PatientImportRowValidator rowValidator;
    private final PatientImportDuplicateDetector duplicateDetector;

    @Override
    public PatientImportPreviewResult preview(PreviewPatientImportCommand command) {
        if (command.fileContent() == null || command.fileContent().length == 0) {
            throw new ValidationException("Tệp tải lên không được để trống.");
        }

        List<RawPatientRowDto> rawRows = sheetParser.parse(new ByteArrayInputStream(command.fileContent()));

        List<ValidatedPatientRowDto> validRows = new ArrayList<>();
        List<PatientImportRowErrorResult> errorResults = new ArrayList<>();

        for (RawPatientRowDto raw : rawRows) {
            PatientImportRowValidator.RowValidationResult result = rowValidator.validate(raw);
            if (result.isValid()) {
                validRows.add(result.validRow());
            } else {
                PatientImportRowError err = result.error();
                errorResults.add(new PatientImportRowErrorResult(
                        err.getRowNumber(),
                        err.getErrorField(),
                        err.getErrorMessage(),
                        err.getRawData()
                ));
            }
        }

        PatientImportDuplicateDetector.DuplicateCheckResult dupResult = duplicateDetector.detectDuplicates(validRows);

        int totalRows = rawRows.size();
        int validCount = dupResult.nonDuplicateRows().size();
        int errorCount = errorResults.size();
        int duplicateCount = dupResult.suspectedDuplicates().size();

        return new PatientImportPreviewResult(
                command.fileName(),
                totalRows,
                validCount,
                errorCount,
                duplicateCount,
                errorResults,
                dupResult.suspectedDuplicates()
        );
    }
}

package com.benhsoan.application.ucservice.patient;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.PatientImportLog;
import com.benhsoan.domain.patient.PatientImportRowError;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.exception.ConcurrentImportInProgressException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.ImportPatientsCommand;
import com.benhsoan.port.dto.result.patient.PatientImportResult;
import com.benhsoan.port.dto.result.patient.PatientImportRowErrorResult;
import com.benhsoan.port.dto.result.patient.SuspectedDuplicateResult;
import com.benhsoan.port.dto.spreadsheet.RawPatientRowDto;
import com.benhsoan.port.inbound.patient.ImportPatientsUseCase;
import com.benhsoan.port.outbound.generator.PatientCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientImportLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.spreadsheet.PatientSpreadsheetParserPort;

@Service
public class ImportPatientsService implements ImportPatientsUseCase {

    private final PatientSpreadsheetParserPort sheetParser;
    private final PatientImportRowValidator rowValidator;
    private final PatientImportDuplicateDetector duplicateDetector;
    private final PatientRepository patientRepository;
    private final PatientImportLogRepository patientImportLogRepository;
    private final PatientChangeLogRepository patientChangeLogRepository;
    private final PatientCodeGenerator patientCodeGenerator;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final PatientChangeDetailBuilder changeDetailBuilder;
    private final ObjectMapper objectMapper;
    private final ReentrantLock importLock;

    @Autowired
    public ImportPatientsService(
            PatientSpreadsheetParserPort sheetParser,
            PatientImportRowValidator rowValidator,
            PatientImportDuplicateDetector duplicateDetector,
            PatientRepository patientRepository,
            PatientImportLogRepository patientImportLogRepository,
            PatientChangeLogRepository patientChangeLogRepository,
            PatientCodeGenerator patientCodeGenerator,
            CurrentUserPort currentUserPort,
            AuditLogRepository auditLogRepository,
            PatientChangeDetailBuilder changeDetailBuilder,
            ObjectMapper objectMapper) {
        this(sheetParser, rowValidator, duplicateDetector, patientRepository, patientImportLogRepository,
                patientChangeLogRepository, patientCodeGenerator, currentUserPort, auditLogRepository,
                changeDetailBuilder, objectMapper, new ReentrantLock());
    }

    public ImportPatientsService(
            PatientSpreadsheetParserPort sheetParser,
            PatientImportRowValidator rowValidator,
            PatientImportDuplicateDetector duplicateDetector,
            PatientRepository patientRepository,
            PatientImportLogRepository patientImportLogRepository,
            PatientChangeLogRepository patientChangeLogRepository,
            PatientCodeGenerator patientCodeGenerator,
            CurrentUserPort currentUserPort,
            AuditLogRepository auditLogRepository,
            PatientChangeDetailBuilder changeDetailBuilder,
            ObjectMapper objectMapper,
            ReentrantLock importLock) {
        this.sheetParser = sheetParser;
        this.rowValidator = rowValidator;
        this.duplicateDetector = duplicateDetector;
        this.patientRepository = patientRepository;
        this.patientImportLogRepository = patientImportLogRepository;
        this.patientChangeLogRepository = patientChangeLogRepository;
        this.patientCodeGenerator = patientCodeGenerator;
        this.currentUserPort = currentUserPort;
        this.auditLogRepository = auditLogRepository;
        this.changeDetailBuilder = changeDetailBuilder;
        this.objectMapper = objectMapper;
        this.importLock = importLock != null ? importLock : new ReentrantLock();
    }

    @Override
    @Transactional
    public PatientImportResult importPatients(ImportPatientsCommand command) {
        if (!importLock.tryLock()) {
            throw new ConcurrentImportInProgressException();
        }
        try {
            if (command.fileContent() == null || command.fileContent().length == 0) {
                throw new ValidationException("Tệp tải lên không được để trống.");
            }

            UUID currentUserId = currentUserPort.getCurrentUserId();
            List<RawPatientRowDto> rawRows = sheetParser.parse(new ByteArrayInputStream(command.fileContent()));

            List<ValidatedPatientRowDto> validRows = new ArrayList<>();
            List<PatientImportRowError> allErrors = new ArrayList<>();

            // 1. Row-level validation
            for (RawPatientRowDto raw : rawRows) {
                PatientImportRowValidator.RowValidationResult result = rowValidator.validate(raw);
                if (result.isValid()) {
                    validRows.add(result.validRow());
                } else {
                    allErrors.add(result.error());
                }
            }

            // 2. Duplicate detection
            PatientImportDuplicateDetector.DuplicateCheckResult dupResult = duplicateDetector
                    .detectDuplicates(validRows);
            List<ValidatedPatientRowDto> rowsToImport;

            if (command.skipDuplicates()) {
                rowsToImport = dupResult.nonDuplicateRows();
                for (SuspectedDuplicateResult dup : dupResult.suspectedDuplicates()) {
                    allErrors.add(PatientImportRowError.create(
                            dup.rowNumber(),
                            dup.identityNumber() != null ? "Số CCCD/CMND" : "Thông tin định danh",
                            "Dòng bị bỏ qua do trùng lặp: " + dup.duplicateReason(),
                            "Họ tên: " + dup.fullName() + ", SĐT: " + dup.phone()));
                }
            } else {
                // When not skipping duplicates, valid rows that are not duplicates are
                // imported,
                // and duplicates are recorded as errors
                rowsToImport = dupResult.nonDuplicateRows();
                for (SuspectedDuplicateResult dup : dupResult.suspectedDuplicates()) {
                    allErrors.add(PatientImportRowError.create(
                            dup.rowNumber(),
                            dup.identityNumber() != null ? "Số CCCD/CMND" : "Thông tin định danh",
                            "Phát hiện nghi trùng hồ sơ: " + dup.duplicateReason(),
                            "Họ tên: " + dup.fullName() + ", SĐT: " + dup.phone()));
                }
            }

            // 3. Persist valid patients
            List<String> createdCodes = new ArrayList<>();
            for (ValidatedPatientRowDto row : rowsToImport) {
                String patientCode = patientCodeGenerator.generate();

                boolean isMinor = com.benhsoan.domain.patient.PatientMinorPolicy.isMinor(row.getDateOfBirth());
                String consentSignerName = isMinor ? row.getGuardianName() : row.getFullName();

                Patient patient = Patient.create(
                        patientCode,
                        row.getFullName(),
                        row.getDateOfBirth(),
                        row.getGender(),
                        row.getPhone(),
                        row.getEmail(),
                        row.getAddress(),
                        row.getIdentityNumber(),
                        row.getInsuranceNumber(),
                        row.getBloodType(),
                        row.getEmergencyContact(),
                        row.getEmergencyRelationship(),
                        row.getEmergencyPhone(),
                        row.getGuardianName(),
                        row.getGuardianRelationship(),
                        row.getGuardianPhone(),
                        null,
                        null,
                        consentSignerName,
                        true, // QTN-24: Auto-consented for legacy import
                        com.benhsoan.domain.patient.PatientConsentVersion.current(),
                        currentUserId);

                Patient saved = patientRepository.save(patient);
                createdCodes.add(saved.getPatientCode());

                String changeDetail = changeDetailBuilder.forCreate(saved);
                PatientChangeLog changeLog = PatientChangeLog.create(
                        saved.getId(),
                        currentUserId,
                        PatientChangeAction.CREATE,
                        changeDetail);
                patientChangeLogRepository.save(changeLog);
            }

            int totalRows = rawRows.size();
            int successCount = createdCodes.size();
            int duplicateCount = dupResult.suspectedDuplicates().size();
            int errorCount = allErrors.size();

            // 4. Save Import Log
            PatientImportLog importLog = PatientImportLog.create(
                    command.fileName(),
                    command.fileSize(),
                    totalRows,
                    successCount,
                    errorCount,
                    duplicateCount,
                    currentUserId,
                    allErrors);
            PatientImportLog savedLog = patientImportLogRepository.save(importLog);

            // 5. Save Admin Audit Log (QTN-31) with safe JSON serialization
            String auditDetail;
            try {
                auditDetail = objectMapper.writeValueAsString(Map.of(
                        "fileName", command.fileName() != null ? command.fileName() : "",
                        "totalRows", totalRows,
                        "successRows", successCount,
                        "errorRows", errorCount,
                        "duplicateRows", duplicateCount));
            } catch (Exception ignored) {
                auditDetail = "{\"fileName\":\"import\",\"totalRows\":%d,\"successRows\":%d,\"errorRows\":%d,\"duplicateRows\":%d}"
                        .formatted(totalRows, successCount, errorCount, duplicateCount);
            }

            auditLogRepository.save(AuditLog.create(
                    currentUserId,
                    ActionType.IMPORT,
                    ResourceType.PATIENT_IMPORT,
                    savedLog.getId(),
                    auditDetail,
                    null));

            // 6. Map response errors
            List<PatientImportRowErrorResult> errorResults = allErrors.stream()
                    .map(err -> new PatientImportRowErrorResult(
                            err.getRowNumber(),
                            err.getErrorField(),
                            err.getErrorMessage(),
                            err.getRawData()))
                    .toList();

            return new PatientImportResult(
                    savedLog.getId(),
                    command.fileName(),
                    totalRows,
                    successCount,
                    errorCount,
                    duplicateCount,
                    createdCodes,
                    errorResults);
        } finally {
            importLock.unlock();
        }
    }
}

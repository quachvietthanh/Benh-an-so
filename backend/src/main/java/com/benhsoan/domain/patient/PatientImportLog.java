package com.benhsoan.domain.patient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.ImportStatus;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientImportLog {

    private UUID id;

    private String fileName;

    private long fileSize;

    private int totalRows;

    private int successRows;

    private int errorRows;

    private int duplicateRows;

    private ImportStatus status;

    private UUID importedBy;

    private Instant createdAt;

    private List<PatientImportRowError> errors = new ArrayList<>();

    private PatientImportLog(
            UUID id,
            String fileName,
            long fileSize,
            int totalRows,
            int successRows,
            int errorRows,
            int duplicateRows,
            ImportStatus status,
            UUID importedBy,
            Instant createdAt,
            List<PatientImportRowError> errors
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.fileName = Objects.requireNonNull(fileName, "fileName must not be null");
        this.fileSize = fileSize;
        this.totalRows = totalRows;
        this.successRows = successRows;
        this.errorRows = errorRows;
        this.duplicateRows = duplicateRows;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.importedBy = Objects.requireNonNull(importedBy, "importedBy must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (errors != null) {
            this.errors = new ArrayList<>(errors);
        }
    }

    public static PatientImportLog create(
            String fileName,
            long fileSize,
            int totalRows,
            int successRows,
            int errorRows,
            int duplicateRows,
            UUID importedBy,
            List<PatientImportRowError> errors
    ) {
        ImportStatus status;
        if (successRows > 0 && errorRows == 0) {
            status = ImportStatus.SUCCESS;
        } else if (successRows > 0) {
            status = ImportStatus.PARTIAL;
        } else {
            status = ImportStatus.FAILED;
        }

        UUID logId = UUID.randomUUID();
        List<PatientImportRowError> assignedErrors = new ArrayList<>();
        if (errors != null) {
            for (PatientImportRowError err : errors) {
                err.assignImportLogId(logId);
                assignedErrors.add(err);
            }
        }

        return new PatientImportLog(
                logId,
                fileName,
                fileSize,
                totalRows,
                successRows,
                errorRows,
                duplicateRows,
                status,
                importedBy,
                Instant.now(),
                assignedErrors
        );
    }

    public static PatientImportLog reconstitute(
            UUID id,
            String fileName,
            long fileSize,
            int totalRows,
            int successRows,
            int errorRows,
            int duplicateRows,
            ImportStatus status,
            UUID importedBy,
            Instant createdAt,
            List<PatientImportRowError> errors
    ) {
        return new PatientImportLog(
                id,
                fileName,
                fileSize,
                totalRows,
                successRows,
                errorRows,
                duplicateRows,
                status,
                importedBy,
                createdAt,
                errors
        );
    }

    public List<PatientImportRowError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}

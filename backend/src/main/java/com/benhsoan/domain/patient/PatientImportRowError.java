package com.benhsoan.domain.patient;

import java.util.Objects;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientImportRowError {

    private UUID id;

    private UUID importLogId;

    private int rowNumber;

    private String errorField;

    private String errorMessage;

    private String rawData;

    private PatientImportRowError(
            UUID id,
            UUID importLogId,
            int rowNumber,
            String errorField,
            String errorMessage,
            String rawData
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.importLogId = importLogId;
        this.rowNumber = rowNumber;
        this.errorField = errorField;
        this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage must not be null");
        this.rawData = rawData;
    }

    public static PatientImportRowError create(
            int rowNumber,
            String errorField,
            String errorMessage,
            String rawData
    ) {
        return new PatientImportRowError(
                UUID.randomUUID(),
                null,
                rowNumber,
                errorField,
                errorMessage,
                rawData
        );
    }

    public static PatientImportRowError reconstitute(
            UUID id,
            UUID importLogId,
            int rowNumber,
            String errorField,
            String errorMessage,
            String rawData
    ) {
        return new PatientImportRowError(
                id,
                importLogId,
                rowNumber,
                errorField,
                errorMessage,
                rawData
        );
    }

    public void assignImportLogId(UUID importLogId) {
        this.importLogId = importLogId;
    }
}

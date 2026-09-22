package com.benhsoan.persistence.entity.patient;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patient_import_log_errors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientImportLogErrorEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_log_id", nullable = false)
    private PatientImportLogEntity importLog;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "error_field", length = 100)
    private String errorField;

    @Column(name = "error_message", nullable = false, length = 500)
    private String errorMessage;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;
}

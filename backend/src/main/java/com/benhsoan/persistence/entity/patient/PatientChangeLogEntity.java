package com.benhsoan.persistence.entity.patient;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.PatientChangeAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patient_change_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientChangeLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "changed_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private PatientChangeAction action;

    @Column(name = "change_detail", nullable = false, columnDefinition = "JSON")
    private String changeDetail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
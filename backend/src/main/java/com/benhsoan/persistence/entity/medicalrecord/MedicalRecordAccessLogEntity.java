package com.benhsoan.persistence.entity.medicalrecord;

import java.time.Instant;
import java.util.UUID;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medical_record_access_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordAccessLogEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID patientId;
    @Column(name = "visit_id", columnDefinition = "BINARY(16)")
    UUID visitId;
    @Column(name = "medical_record_id", columnDefinition = "BINARY(16)")
    UUID medicalRecordId;
    @Column(name = "accessed_by", nullable = false, columnDefinition = "BINARY(16)")
    UUID accessedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    MedicalRecordAccessAction action;
    @Column(length = 500)
    String detail;
    @Column(name = "ip_address", length = 45)
    String ipAddress;
    @Column(name = "accessed_at", nullable = false)
    Instant accessedAt;
}

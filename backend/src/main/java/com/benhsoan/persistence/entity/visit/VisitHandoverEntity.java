package com.benhsoan.persistence.entity.visit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "visit_handovers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitHandoverEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "visit_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID visitId;

    @Column(name = "from_doctor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID fromDoctorId;

    @Column(name = "to_doctor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID toDoctorId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "handed_over_at", nullable = false)
    private Instant handedOverAt;

    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

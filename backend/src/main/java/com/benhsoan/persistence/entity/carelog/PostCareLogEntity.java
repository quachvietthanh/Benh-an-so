package com.benhsoan.persistence.entity.carelog;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;

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
@Table(name = "post_care_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCareLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "reminder_id", columnDefinition = "BINARY(16)")
    private UUID reminderId;

    @Column(name = "visit_id", columnDefinition = "BINARY(16)")
    private UUID visitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_channel", nullable = false, length = 30)
    private ContactChannel contactChannel;

    @Column(name = "contacted_at", nullable = false)
    private Instant contactedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_condition", nullable = false, length = 30)
    private PatientCondition patientCondition;

    @Column(name = "care_notes", nullable = false, columnDefinition = "TEXT")
    private String careNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_outcome", nullable = false, length = 30)
    private ContactOutcome contactOutcome;

    @Column(name = "performed_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID performedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

package com.benhsoan.domain.patient;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientAllergyChangeLog {

    private UUID id;
    private UUID allergyId;
    private UUID patientId;
    private String action;
    private String beforeData;
    private String afterData;
    private String changeReason;
    private UUID changedBy;
    private Instant changedAt;

    private PatientAllergyChangeLog(
            UUID id,
            UUID allergyId,
            UUID patientId,
            String action,
            String beforeData,
            String afterData,
            String changeReason,
            UUID changedBy,
            Instant changedAt
    ) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.allergyId = Guard.require(allergyId, "Allergy ID");
        this.patientId = Guard.require(patientId, "Patient ID");
        this.action = Guard.require(action, "Action");
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.changeReason = changeReason;
        this.changedBy = Guard.require(changedBy, "Changed by");
        this.changedAt = Objects.requireNonNull(changedAt, "Changed at cannot be null");
    }

    public static PatientAllergyChangeLog create(
            UUID allergyId,
            UUID patientId,
            String action,
            String beforeData,
            String afterData,
            String changeReason,
            UUID changedBy,
            Instant changedAt
    ) {
        return new PatientAllergyChangeLog(
                UUID.randomUUID(),
                allergyId,
                patientId,
                action,
                beforeData,
                afterData,
                changeReason,
                changedBy,
                changedAt != null ? changedAt : Instant.now()
        );
    }

    public static PatientAllergyChangeLog restore(
            UUID id,
            UUID allergyId,
            UUID patientId,
            String action,
            String beforeData,
            String afterData,
            String changeReason,
            UUID changedBy,
            Instant changedAt
    ) {
        return new PatientAllergyChangeLog(
                id,
                allergyId,
                patientId,
                action,
                beforeData,
                afterData,
                changeReason,
                changedBy,
                changedAt
        );
    }
}

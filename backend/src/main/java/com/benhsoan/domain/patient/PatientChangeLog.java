package com.benhsoan.domain.patient;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.PatientChangeAction;
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
public class PatientChangeLog {

    private UUID id;

    private UUID patientId;

    private UUID changedBy;

    private PatientChangeAction action;

    private String changeDetail;

    private Instant createdAt;

    private PatientChangeLog(
            UUID id,
            UUID patientId,
            UUID changedBy,
            PatientChangeAction action,
            String detail,
            Instant createdAt
    ) {

        this.id = Objects.requireNonNull(id);

        this.patientId = Guard.require(patientId, "Patient");

        this.changedBy = Guard.require(changedBy, "Changed by");

        this.action = Guard.require(action, "Action");

        this.changeDetail = detail;

        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static PatientChangeLog create(
            UUID patientId,
            UUID changedBy,
            PatientChangeAction action,
            String detail
    ) {

        return new PatientChangeLog(
                UUID.randomUUID(),
                patientId,
                changedBy,
                action,
                detail,
                Instant.now()
        );
    }

    public static PatientChangeLog restore(
            UUID id,
            UUID patientId,
            UUID changedBy,
            PatientChangeAction action,
            String detail,
            Instant createdAt
    ) {

        return new PatientChangeLog(
                id,
                patientId,
                changedBy,
                action,
                detail,
                createdAt
        );
    }

}
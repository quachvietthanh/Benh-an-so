package com.benhsoan.domain.carelog;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;
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
public class PostCareLog {

    private UUID id;
    private UUID patientId;
    private UUID reminderId;
    private UUID visitId;
    private ContactChannel contactChannel;
    private Instant contactedAt;
    private PatientCondition patientCondition;
    private String careNotes;
    private ContactOutcome contactOutcome;
    private UUID performedBy;
    private Instant createdAt;

    private PostCareLog(
            UUID id,
            UUID patientId,
            UUID reminderId,
            UUID visitId,
            ContactChannel contactChannel,
            Instant contactedAt,
            PatientCondition patientCondition,
            String careNotes,
            ContactOutcome contactOutcome,
            UUID performedBy,
            Instant createdAt
    ) {
        this.id = Guard.require(id, "Care log id");
        this.patientId = Guard.require(patientId, "Patient id");
        this.contactChannel = Guard.require(contactChannel, "Contact channel");
        this.contactedAt = Guard.require(contactedAt, "Contacted at");
        this.patientCondition = Guard.require(patientCondition, "Patient condition");
        this.careNotes = Guard.require(careNotes, "Care notes");
        this.contactOutcome = Guard.require(contactOutcome, "Contact outcome");
        this.performedBy = Guard.require(performedBy, "Performed by");
        this.createdAt = Guard.require(createdAt, "Created at");

        this.reminderId = reminderId;
        this.visitId = visitId;
    }

    public static PostCareLog create(
            UUID patientId,
            UUID reminderId,
            UUID visitId,
            ContactChannel contactChannel,
            Instant contactedAt,
            PatientCondition patientCondition,
            String careNotes,
            ContactOutcome contactOutcome,
            UUID performedBy,
            Instant createdAt
    ) {
        return new PostCareLog(
                UUID.randomUUID(),
                patientId,
                reminderId,
                visitId,
                contactChannel,
                contactedAt,
                patientCondition,
                careNotes,
                contactOutcome,
                performedBy,
                createdAt
        );
    }

    public static PostCareLog restore(
            UUID id,
            UUID patientId,
            UUID reminderId,
            UUID visitId,
            ContactChannel contactChannel,
            Instant contactedAt,
            PatientCondition patientCondition,
            String careNotes,
            ContactOutcome contactOutcome,
            UUID performedBy,
            Instant createdAt
    ) {
        return new PostCareLog(
                id,
                patientId,
                reminderId,
                visitId,
                contactChannel,
                contactedAt,
                patientCondition,
                careNotes,
                contactOutcome,
                performedBy,
                createdAt
        );
    }
}

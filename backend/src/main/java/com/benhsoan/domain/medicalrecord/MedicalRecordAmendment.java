package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalRecordAmendment {

    private UUID id, medicalRecordId, amendedBy;
    private String content, reason;
    private Instant amendedAt;

    private MedicalRecordAmendment(UUID id, UUID recordId, String content, String reason, UUID by, Instant at) {
        this.id = Objects.requireNonNull(id);
        medicalRecordId = Objects.requireNonNull(recordId);
        this.content = Guard.require(content, "Amendment content");
        this.reason = Guard.require(reason, "Amendment reason");
        amendedBy = Objects.requireNonNull(by);
        amendedAt = Objects.requireNonNull(at);
    }

    public static MedicalRecordAmendment create(UUID recordId, String content, String reason, UUID by, Instant at) {
        return new MedicalRecordAmendment(UUID.randomUUID(), recordId, content, reason, by, at);
    }

    public static MedicalRecordAmendment restore(UUID id, UUID recordId, String content, String reason, UUID by, Instant at) {
        return new MedicalRecordAmendment(id, recordId, content, reason, by, at);
    }
}

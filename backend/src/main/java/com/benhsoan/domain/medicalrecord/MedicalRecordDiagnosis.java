package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalRecordDiagnosis {

    private UUID id, medicalRecordId, diagnosisCatalogId, diagnosedBy;
    private String diagnosisCode, diagnosisName, note;
    private DiagnosisType diagnosisType;
    private Instant diagnosedAt, createdAt, updatedAt;

    private MedicalRecordDiagnosis(UUID id, UUID r, UUID c, String code, String name, DiagnosisType t, String note, UUID by, Instant at, Instant created, Instant updated) {
        this.id = Objects.requireNonNull(id);
        medicalRecordId = Objects.requireNonNull(r);
        diagnosisCatalogId = c;
        diagnosisCode = code;
        diagnosisName = Guard.require(name, "Diagnosis name");
        diagnosisType = Objects.requireNonNull(t);
        this.note = note;
        diagnosedBy = Objects.requireNonNull(by);
        diagnosedAt = Objects.requireNonNull(at);
        createdAt = Objects.requireNonNull(created);
        updatedAt = updated;
    }

    public static MedicalRecordDiagnosis create(UUID r, UUID c, String code, String name, DiagnosisType t, String note, UUID by, Instant at) {
        return new MedicalRecordDiagnosis(UUID.randomUUID(), r, c, code, name, t, note, by, at, at, null);
    }

    public static MedicalRecordDiagnosis restore(UUID id, UUID r, UUID c, String code, String name, DiagnosisType t, String note, UUID by, Instant at, Instant created, Instant updated) {
        return new MedicalRecordDiagnosis(id, r, c, code, name, t, note, by, at, created, updated);
    }

    public void updateNote(String note, Instant at) {
        this.note = note;
        updatedAt = Objects.requireNonNull(at);
    }

    public void changeType(DiagnosisType type, Instant at) {
        diagnosisType = Objects.requireNonNull(type);
        updatedAt = Objects.requireNonNull(at);
    }
}

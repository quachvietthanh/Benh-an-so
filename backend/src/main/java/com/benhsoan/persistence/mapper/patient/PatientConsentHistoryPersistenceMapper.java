package com.benhsoan.persistence.mapper.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientConsentRecord;
import com.benhsoan.domain.patient.enums.ConsentScope;
import com.benhsoan.persistence.entity.patient.PatientConsentHistoryEntity;

@Component
public class PatientConsentHistoryPersistenceMapper {

    public PatientConsentRecord toDomain(PatientConsentHistoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return PatientConsentRecord.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getVersionNumber(),
                entity.getVersionCode(),
                entity.getStatus(),
                ConsentScope.parse(entity.getScopes()),
                entity.isConsentAgreed(),
                entity.getConsentAgreedAt(),
                entity.isConsentWithdrawn(),
                entity.getConsentWithdrawnAt(),
                entity.getConsentWithdrawnReason(),
                entity.isNonMedicalUseRestricted(),
                entity.getSignerName(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }

    public PatientConsentHistoryEntity toEntity(PatientConsentRecord domain) {
        if (domain == null) {
            return null;
        }

        return PatientConsentHistoryEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .versionNumber(domain.getVersionNumber())
                .versionCode(domain.getVersionCode())
                .status(domain.getStatus())
                .scopes(ConsentScope.toCommaSeparated(domain.getScopes()))
                .consentAgreed(domain.isConsentAgreed())
                .consentAgreedAt(domain.getConsentAgreedAt())
                .consentWithdrawn(domain.isConsentWithdrawn())
                .consentWithdrawnAt(domain.getConsentWithdrawnAt())
                .consentWithdrawnReason(domain.getConsentWithdrawnReason())
                .nonMedicalUseRestricted(domain.isNonMedicalUseRestricted())
                .signerName(domain.getSignerName())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

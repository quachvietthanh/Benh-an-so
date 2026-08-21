package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Central retention policy used by the medical-record deletion guard (QTN-19).
 */
@Component
@RequiredArgsConstructor
public class MedicalRecordRetentionPolicy {

    private final ClinicConfigurationRepository clinicConfigurationRepository;

    public int retentionYears() {
        return clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getRetentionYears)
                .orElse(ClinicConfiguration.DEFAULT_RETENTION_YEARS);
    }

    public Instant expirationDate(Visit visit) {
        Instant endedAt = visit.getCompletedAt();
        if (endedAt == null) {
            endedAt = visit.getUpdatedAt();
        }
        if (endedAt == null) {
            endedAt = visit.getVisitAt();
        }
        return endedAt.atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .plusYears(retentionYears())
                .toInstant(ZoneOffset.UTC);
    }

    public boolean isWithinRetention(Visit visit, Instant now) {
        return now.isBefore(expirationDate(visit));
    }
}

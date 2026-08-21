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

    /**
     * QTN-19: expiration is computed strictly from the visit completion date.
     * An uncompleted visit (no completedAt) is always treated as within retention.
     */
    public boolean isWithinRetention(Visit visit, Instant now) {
        Instant endedAt = visit.getCompletedAt();
        if (endedAt == null || !visit.isCompleted()) {
            return true;
        }
        Instant expiration = endedAt.atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .plusYears(retentionYears())
                .toInstant(ZoneOffset.UTC);
        return now.isBefore(expiration);
    }
}

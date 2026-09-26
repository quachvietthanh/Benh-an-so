package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.prescription.PrescriptionMaxDailyDoseWarningLogEntity;

public interface JpaPrescriptionMaxDailyDoseWarningLogRepository
        extends JpaRepository<PrescriptionMaxDailyDoseWarningLogEntity, UUID> {
}

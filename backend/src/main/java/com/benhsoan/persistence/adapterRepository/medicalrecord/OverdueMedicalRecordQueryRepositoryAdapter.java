package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaOverdueMedicalRecordRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.OverdueMedicalRecordProjection;
import com.benhsoan.port.dto.result.OverdueMedicalRecordResult;
import com.benhsoan.port.outbound.repository.medicalrecord.OverdueMedicalRecordQueryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OverdueMedicalRecordQueryRepositoryAdapter implements OverdueMedicalRecordQueryRepository {

    private final JpaOverdueMedicalRecordRepository jpaRepository;

    @Override
    public Page<OverdueMedicalRecordResult> findOverdueRecords(
            UUID doctorId,
            Instant thresholdCompletedAt,
            int signingDeadlineHours,
            Instant now,
            Pageable pageable
    ) {
        return jpaRepository.findOverdueRecords(thresholdCompletedAt, doctorId, pageable)
                .map(projection -> toResult(projection, signingDeadlineHours, now));
    }

    private OverdueMedicalRecordResult toResult(
            OverdueMedicalRecordProjection p,
            int signingDeadlineHours,
            Instant now
    ) {
        Instant deadlineAt = p.visitCompletedAt() != null
                ? p.visitCompletedAt().plus(Duration.ofHours(signingDeadlineHours))
                : now;
        long overdueHours = Math.max(0, Duration.between(deadlineAt, now).toHours());
        long reminderCount = p.reminderCount() != null ? p.reminderCount() : 0L;

        return new OverdueMedicalRecordResult(
                p.medicalRecordId(),
                p.status(),
                p.visitId(),
                p.visitCode(),
                p.visitCompletedAt(),
                p.patientId(),
                p.patientCode(),
                p.patientFullName(),
                p.doctorId(),
                p.doctorFullName(),
                p.doctorEmail(),
                p.doctorPhone(),
                signingDeadlineHours,
                deadlineAt,
                overdueHours,
                reminderCount,
                p.lastRemindedAt()
        );
    }
}

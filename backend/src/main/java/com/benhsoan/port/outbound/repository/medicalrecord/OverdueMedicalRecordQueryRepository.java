package com.benhsoan.port.outbound.repository.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.OverdueMedicalRecordResult;

public interface OverdueMedicalRecordQueryRepository {

    Page<OverdueMedicalRecordResult> findOverdueRecords(
            UUID doctorId,
            Instant thresholdCompletedAt,
            int signingDeadlineHours,
            Instant now,
            Pageable pageable
    );
}

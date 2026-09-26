package com.benhsoan.port.outbound.repository.medicalrecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.ArchiveEligibleMedicalRecordResult;
import com.benhsoan.port.dto.result.ArchivedMedicalRecordResult;

public interface ArchiveMedicalRecordQueryRepository {

    Page<ArchiveEligibleMedicalRecordResult> findEligibleForArchive(
            Instant completedBefore,
            Pageable pageable
    );

    List<UUID> findAllEligibleMedicalRecordIds(
            Instant completedBefore
    );

    Page<ArchivedMedicalRecordResult> searchArchivedRecords(
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            UUID doctorId,
            Pageable pageable
    );
}

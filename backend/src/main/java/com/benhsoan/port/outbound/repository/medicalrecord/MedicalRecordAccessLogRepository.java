package com.benhsoan.port.outbound.repository.medicalrecord;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;

public interface MedicalRecordAccessLogRepository {

    MedicalRecordAccessLog save(MedicalRecordAccessLog accessLog);

    Page<MedicalRecordAccessLog> search(
            GetMedicalRecordAccessLogsQuery query,
            Pageable pageable
    );

    List<MedicalRecordAccessLog> findViewsBetween(Instant from, Instant to);
}

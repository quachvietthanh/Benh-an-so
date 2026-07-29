package com.benhsoan.port.outbound.repository.logRepository;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;

public interface MedicalRecordAccessLogRepository {

    MedicalRecordAccessLog save(MedicalRecordAccessLog accessLog);
}

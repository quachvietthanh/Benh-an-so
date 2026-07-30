package com.benhsoan.port.outbound.repository.crudRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;

public interface MedicalRecordAmendmentRepository {

    MedicalRecordAmendment save(MedicalRecordAmendment amendment);

    List<MedicalRecordAmendment> findByMedicalRecordId(UUID medicalRecordId);
}

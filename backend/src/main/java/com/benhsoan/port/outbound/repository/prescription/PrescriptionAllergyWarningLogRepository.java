package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.prescription.PrescriptionAllergyWarningLog;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionAllergyWarningLogsQuery;

public interface PrescriptionAllergyWarningLogRepository {

    PrescriptionAllergyWarningLog save(PrescriptionAllergyWarningLog warningLog);

    List<PrescriptionAllergyWarningLog> findByPrescriptionId(UUID prescriptionId);

    Page<PrescriptionAllergyWarningLog> search(
            SearchPrescriptionAllergyWarningLogsQuery query,
            Pageable pageable
    );
}

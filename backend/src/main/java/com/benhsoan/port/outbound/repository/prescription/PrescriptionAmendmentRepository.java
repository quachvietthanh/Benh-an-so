package com.benhsoan.port.outbound.repository.logRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.prescription.PrescriptionAmendment;

public interface PrescriptionAmendmentRepository {

    PrescriptionAmendment save(PrescriptionAmendment amendment);

    Optional<PrescriptionAmendment> findById(UUID id);

    List<PrescriptionAmendment> findByPrescriptionId(UUID prescriptionId);
}

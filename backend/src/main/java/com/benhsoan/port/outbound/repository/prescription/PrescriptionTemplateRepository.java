package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.prescription.PrescriptionTemplate;

public interface PrescriptionTemplateRepository {

    PrescriptionTemplate save(PrescriptionTemplate template);

    Optional<PrescriptionTemplate> findById(UUID id);

    List<PrescriptionTemplate> findByDiagnosisCatalogIdAndCreatedBy(
            UUID diagnosisCatalogId,
            UUID createdBy
    );

    List<PrescriptionTemplate> findByDiagnosisCatalogId(UUID diagnosisCatalogId);
}

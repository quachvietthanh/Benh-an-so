package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.prescription.PrescriptionDispenseItem;

public interface PrescriptionDispenseItemRepository {

    PrescriptionDispenseItem save(PrescriptionDispenseItem dispenseItem);

    List<PrescriptionDispenseItem> saveAll(List<PrescriptionDispenseItem> dispenseItems);

    Optional<PrescriptionDispenseItem> findById(UUID id);

    List<PrescriptionDispenseItem> findByPrescriptionId(UUID prescriptionId);

    List<PrescriptionDispenseItem> findByPrescriptionItemId(UUID prescriptionItemId);
}

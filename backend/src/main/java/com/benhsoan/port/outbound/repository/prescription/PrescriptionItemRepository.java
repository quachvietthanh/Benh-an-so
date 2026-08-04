package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.PrescriptionItem;
public interface PrescriptionItemRepository {

    List<PrescriptionItem> findByPrescriptionId(UUID prescriptionId);

    void deleteAllByPrescriptionId(UUID prescriptionId);
}

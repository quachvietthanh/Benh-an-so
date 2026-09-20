package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.MedicationReturn;

public interface MedicationReturnRepository {

    MedicationReturn save(MedicationReturn medicationReturn);

    List<MedicationReturn> findByPrescriptionId(UUID prescriptionId);
}

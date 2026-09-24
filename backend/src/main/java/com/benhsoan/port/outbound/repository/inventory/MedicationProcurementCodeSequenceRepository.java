package com.benhsoan.port.outbound.repository.inventory;

public interface MedicationProcurementCodeSequenceRepository {

    long reserveNextValue(String prefix);
}

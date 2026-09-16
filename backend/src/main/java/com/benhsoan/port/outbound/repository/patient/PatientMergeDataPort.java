package com.benhsoan.port.outbound.repository.patient;

import java.util.UUID;

public interface PatientMergeDataPort {

    int transferAllPatientData(UUID sourcePatientId, UUID targetPatientId);

    boolean hasFinalizedMedicalRecords(UUID patientId);
}

package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

public interface DeleteMedicalRecordUseCase {

    void delete(UUID medicalRecordId);
}

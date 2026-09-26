package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

public interface DeleteDiagnosisCatalogUseCase {

    void delete(UUID diagnosisCatalogId);
}

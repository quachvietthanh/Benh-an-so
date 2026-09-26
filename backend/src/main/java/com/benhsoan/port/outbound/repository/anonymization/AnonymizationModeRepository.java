package com.benhsoan.port.outbound.repository.anonymization;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.anonymization.AnonymizationMode;

public interface AnonymizationModeRepository {

    Optional<AnonymizationMode> find();

    AnonymizationMode save(AnonymizationMode mode, UUID updatedBy);
}

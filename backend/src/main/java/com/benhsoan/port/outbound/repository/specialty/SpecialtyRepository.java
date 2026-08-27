package com.benhsoan.port.outbound.repository.specialty;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.specialty.Specialty;

public interface SpecialtyRepository {

    Optional<Specialty> findById(UUID id);

    List<Specialty> findAllById(Collection<UUID> ids);

    List<Specialty> findByActive(boolean active);
}

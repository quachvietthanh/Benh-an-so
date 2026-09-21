package com.benhsoan.port.outbound.repository.specialty;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.specialty.Specialty;

public interface SpecialtyRepository {

    Specialty save(Specialty specialty);

    Optional<Specialty> findById(UUID id);

    Optional<Specialty> findByCode(String code);

    List<Specialty> findAllById(Collection<UUID> ids);

    List<Specialty> findByActive(boolean active);

    List<Specialty> findAll();

    List<Specialty> search(String keyword, Boolean active);

    boolean existsByCode(String code);

    boolean existsByNameKey(String nameKey);

    boolean existsByNameKeyAndIdNot(String nameKey, UUID id);
}

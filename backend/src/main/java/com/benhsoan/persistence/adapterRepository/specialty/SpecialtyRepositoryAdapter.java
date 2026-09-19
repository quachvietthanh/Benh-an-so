package com.benhsoan.persistence.adapterRepository.specialty;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.persistence.jpaRepository.specialty.JpaSpecialtyRepository;
import com.benhsoan.persistence.mapper.specialty.SpecialtyPersistenceMapper;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SpecialtyRepositoryAdapter implements SpecialtyRepository {

    private final JpaSpecialtyRepository jpaRepository;
    private final SpecialtyPersistenceMapper mapper;

    @Override
    public Specialty save(Specialty specialty) {
        var entity = mapper.toEntity(specialty);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Specialty> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Specialty> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Specialty> findAllById(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Specialty> findByActive(boolean active) {
        return jpaRepository.findByActiveOrderByCodeAsc(active).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Specialty> findAll() {
        return jpaRepository.findAllByOrderByCodeAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Specialty> search(String keyword, Boolean active) {
        return jpaRepository.search(keyword, active).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public boolean existsByNameKey(String nameKey) {
        return jpaRepository.existsByNameKey(nameKey);
    }

    @Override
    public boolean existsByNameKeyAndIdNot(String nameKey, UUID id) {
        return jpaRepository.existsByNameKeyAndIdNot(nameKey, id);
    }
}

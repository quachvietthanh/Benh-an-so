package com.benhsoan.persistence.adapterRepository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.persistence.entity.auth.RoleEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaRoleRepository;
import com.benhsoan.persistence.mapper.auth.RolePersistenceMapper;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final JpaRoleRepository jpaRepository;
    private final RolePersistenceMapper mapper;

    @Override
    public Optional<Role> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRepository.findByName(name)
                .map(mapper::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity = mapper.toEntity(role);
        RoleEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }
}

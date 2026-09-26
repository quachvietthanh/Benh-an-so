package com.benhsoan.persistence.adapterRepository.auth;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.auth.Permission;
import com.benhsoan.persistence.jpaRepository.auth.JpaPermissionRepository;
import com.benhsoan.persistence.mapper.auth.PermissionPersistenceMapper;
import com.benhsoan.port.outbound.repository.auth.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepository {
    private final JpaPermissionRepository jpaRepository;
    private final PermissionPersistenceMapper mapper;

    @Override public Optional<Permission> findByCode(String code) { return jpaRepository.findByCode(code).map(mapper::toDomain); }
    @Override public List<Permission> findAllByCodes(Collection<String> codes) { return jpaRepository.findAllByCodeIn(codes).stream().map(mapper::toDomain).toList(); }
    @Override public List<Permission> findAllActive() { return jpaRepository.findAllByActiveTrueOrderByModuleAscCodeAsc().stream().map(mapper::toDomain).toList(); }
}

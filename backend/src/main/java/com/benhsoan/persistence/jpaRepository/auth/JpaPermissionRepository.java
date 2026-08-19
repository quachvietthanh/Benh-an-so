package com.benhsoan.persistence.jpaRepository.auth;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.auth.PermissionEntity;

public interface JpaPermissionRepository extends JpaRepository<PermissionEntity, UUID> {
    Optional<PermissionEntity> findByCode(String code);
    List<PermissionEntity> findAllByCodeIn(Collection<String> codes);
    List<PermissionEntity> findAllByActiveTrueOrderByModuleAscCodeAsc();
}

package com.benhsoan.persistence.jpaRepository.auth;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.auth.RoleEntity;

public interface JpaRoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByName(String name);

    boolean existsByName(String name);

    List<RoleEntity> findAllByIsSystemTrueOrderByNameAsc();
}

package com.benhsoan.persistence.jpaRepository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.auth.UserEntity;

public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByPhone(String phone);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<UserEntity> findAllByRoleIdAndActiveTrue(UUID roleId);

    long countByRoleIdAndActiveTrue(UUID roleId);
}

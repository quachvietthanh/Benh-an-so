package com.benhsoan.persistence.jpaRepository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.auth.UserEntity;

import jakarta.persistence.LockModeType;

public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByPhone(String phone);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    List<UserEntity> findAllByRoleIdAndActiveTrue(UUID roleId);

    long countByRoleIdAndActiveTrue(UUID roleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);
}

package com.benhsoan.persistence.jpaRepository.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.auth.LoginAttemptEntity;

public interface JpaLoginAttemptRepository extends JpaRepository<LoginAttemptEntity, String> {
}

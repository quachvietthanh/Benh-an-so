package com.benhsoan.persistence.jpaRepository.security;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.persistence.entity.security.SecurityAlertEntity;

public interface JpaSecurityAlertRepository extends JpaRepository<SecurityAlertEntity, String> {

    List<SecurityAlertEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByUserIdAndAlertTypeAndWindowStart(
            String userId,
            AlertType alertType,
            LocalDateTime windowStart
    );
}

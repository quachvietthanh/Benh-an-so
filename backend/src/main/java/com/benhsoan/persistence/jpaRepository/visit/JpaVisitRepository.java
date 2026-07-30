package com.benhsoan.persistence.jpaRepository.visit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.visit.VisitEntity;

public interface JpaVisitRepository extends JpaRepository<VisitEntity, UUID> {

    Optional<VisitEntity> findByVisitCode(String visitCode);
}

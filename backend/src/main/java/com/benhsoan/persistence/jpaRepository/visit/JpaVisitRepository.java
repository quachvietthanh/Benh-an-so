package com.benhsoan.persistence.jpaRepository.visit;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.domain.visit.enums.VisitStatus;

public interface JpaVisitRepository extends JpaRepository<VisitEntity, UUID> {

    Optional<VisitEntity> findByVisitCode(String visitCode);

    Optional<VisitEntity> findTopByOrderByVisitCodeDesc();

    List<VisitEntity> findByPatientIdOrderByVisitAtDesc(UUID patientId);

    boolean existsByPatientIdAndStatusIn(UUID patientId, Collection<VisitStatus> statuses);
}

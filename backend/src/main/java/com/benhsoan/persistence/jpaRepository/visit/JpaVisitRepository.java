package com.benhsoan.persistence.jpaRepository.visit;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.domain.visit.enums.VisitStatus;

public interface JpaVisitRepository extends JpaRepository<VisitEntity, UUID> {

    Optional<VisitEntity> findByVisitCode(String visitCode);

    Optional<VisitEntity> findTopByOrderByVisitCodeDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select visit from VisitEntity visit where visit.id = :visitId")
    Optional<VisitEntity> findByIdForUpdate(@Param("visitId") UUID visitId);

    List<VisitEntity> findByPatientIdOrderByVisitAtDesc(UUID patientId);

    boolean existsByPatientIdAndStatusIn(UUID patientId, Collection<VisitStatus> statuses);
}

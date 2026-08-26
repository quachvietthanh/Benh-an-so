package com.benhsoan.persistence.jpaRepository.patient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.benhsoan.persistence.entity.patient.PatientEntity;

public interface JpaPatientRepository extends JpaRepository<PatientEntity, UUID>, JpaSpecificationExecutor<PatientEntity> {

    Optional<PatientEntity> findByPatientCode(String patientCode);

    Page<PatientEntity> findByFullNameContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    boolean existsByPatientCode(String patientCode);

    boolean existsByIdentityNumber(String identityNumber);

    Optional<PatientEntity> findTopByOrderByPatientCodeDesc();

    boolean existsByIdentityNumberAndIdNot( String identityNumber, UUID id);

    Optional<PatientEntity> findByUserId(UUID userId);

    Optional<PatientEntity> findByPhone(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select patient from PatientEntity patient where patient.id = :patientId")
    Optional<PatientEntity> findByIdForUpdate(@Param("patientId") UUID patientId);

}

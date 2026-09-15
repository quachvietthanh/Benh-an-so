package com.benhsoan.persistence.jpaRepository.patient;

import java.time.LocalDate;
import java.util.List;
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

    List<PatientEntity> findAllByPhone(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select patient from PatientEntity patient where patient.id = :patientId")
    Optional<PatientEntity> findByIdForUpdate(@Param("patientId") UUID patientId);

    @Query("""
        SELECT p FROM PatientEntity p
        WHERE p.status = 'ACTIVE'
          AND p.phone IS NOT NULL
          AND TRIM(p.phone) <> ''
          AND EXISTS (
              SELECT 1 FROM PatientEntity p2
              WHERE p2.id <> p.id
                AND p2.status = 'ACTIVE'
                AND LOWER(p2.fullName) = LOWER(p.fullName)
                AND p2.dateOfBirth = p.dateOfBirth
                AND REPLACE(p2.phone, ' ', '') = REPLACE(p.phone, ' ', '')
          )
        ORDER BY p.fullName ASC, p.dateOfBirth ASC, p.createdAt ASC
    """)
    List<PatientEntity> findSuspectedDuplicates();

    List<PatientEntity> findAllByFullNameIgnoreCaseAndDateOfBirthAndPhone(
            String fullName,
            LocalDate dateOfBirth,
            String phone
    );
}

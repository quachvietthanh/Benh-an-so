package com.benhsoan.port.outbound.repository.patient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.command.patient.SearchPatientCommand;
public interface PatientRepository {

    Optional<Patient> findById(UUID id);

    Patient save(Patient patient);

    Page<Patient> findAll(Pageable pageable);

    Optional<Patient> findByPatientCode(String patientCode);

    boolean existsByPatientCode(String patientCode);

    boolean existsByIdentityNumber(String identityNumber);

    Optional<Patient> findTopByOrderByPatientCodeDesc();

    boolean existsByIdentityNumberAndIdNot( String identityNumber, UUID id);

    Optional<Patient> findByUserId(UUID userId);

    Optional<Patient> findByIdForUpdate(UUID patientId);
    
    Page<Patient> search( SearchPatientCommand command);
}

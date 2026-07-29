package com.benhsoan.application.ucservice.queries.medicalrecord;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.Visit;
import com.benhsoan.domain.patient.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;
import com.benhsoan.port.outbound.repository.crudRepository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.crudRepository.patient.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetPatientMedicalHistoryQueryHandler {

    private final PatientRepository patientRepository;

    private final VisitRepository visitRepository;

    private final CurrentUserPort currentUserPort;

    private final MedicalRecordAuditService auditService;

    public Page<Visit> handle(GetPatientMedicalHistoryQuery query) {

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Set<String> roles = currentUserPort.getCurrentUserRoles();

        // 1. Check patient existence
        Patient patient = patientRepository.findById(query.patientId())
                .orElseThrow(() -> new PatientNotFoundException(
                        query.patientId()
                ));

        // 2. Permission check (business rule)
        boolean isAdmin = roles.contains("ADMIN");
        boolean isPatient = roles.contains("PATIENT");
        boolean isDoctor = roles.contains("DOCTOR");

        boolean isPatientSelf = isPatient
                && patient.getUserId() != null
                && patient.getUserId().equals(currentUserId);

        boolean isDoctorOfPatient = isDoctor
                && visitRepository.existsByPatientIdAndDoctorId(
                        query.patientId(), currentUserId);

        if (!isAdmin && !isPatientSelf && !isDoctorOfPatient) {
            log.warn(
                    "Access denied: user={} attempted to view history "
                            + "of patient={}, roles={}",
                    currentUserId, query.patientId(), roles
            );
            auditService.logAccessDenied(
                    query.patientId(),
                    currentUserId,
                    "Insufficient permissions"
            );
            throw new MedicalRecordAccessDeniedException(
                    "You do not have permission to view "
                            + "this patient's medical history"
            );
        }

        // 3. Build pageable — newest first
        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.DESC, "visitAt")
        );

        // 4. Query with optional date filter
        Page<Visit> visits = visitRepository.findByPatientIdWithDateFilter(
                query.patientId(),
                query.fromDate(),
                query.toDate(),
                pageable
        );

        // 5. Audit log — record that this user accessed the medical history
        auditService.logViewHistory(
                query.patientId(),
                currentUserId
        );

        log.debug(
                "Medical history queried for patient={}, page={}, total={}",
                query.patientId(), query.page(), visits.getTotalElements()
        );

        return visits;
    }
}

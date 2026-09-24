package com.benhsoan.application.ucservice.patient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.result.patient.LinkedPatientProfileResult;
import com.benhsoan.port.inbound.patient.GetPatientLinkedProfilesUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-010 CV-02 / TC-01: resolves every patient profile the authenticated portal
 * account may act for. Scope comes exclusively from the server-side identity: the account's
 * own patient record plus patients whose {@code guardian_user_id} equals that account.
 * QTN-23 / QTN-44: only minors keep a guardian, so the relationship label is the persisted
 * {@code guardianRelationship}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientLinkedProfilesService implements GetPatientLinkedProfilesUseCase {

    static final String SELF_RELATIONSHIP = "SELF";

    private final PatientRepository patientRepository;

    private final CurrentUserPort currentUserPort;

    @Override
    public List<LinkedPatientProfileResult> getLinkedProfiles() {
        UUID userId = currentUserPort.getCurrentUserId();

        List<Patient> linked = new ArrayList<>();

        patientRepository.findByUserId(userId).ifPresent(linked::add);
        linked.addAll(patientRepository.findAllByGuardianUserIdOrderByFullNameAsc(userId));

        return linked.stream()
                .distinct()
                .map(patient -> toResult(patient, !userId.equals(patient.getUserId())))
                .toList();
    }

    private LinkedPatientProfileResult toResult(Patient patient, boolean dependent) {
        return new LinkedPatientProfileResult(
                patient.getId(),
                patient.getPatientCode(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getAge(),
                patient.isMinor(),
                dependent ? patient.getGuardianRelationship() : SELF_RELATIONSHIP,
                !dependent,
                patient.requiresAdultTransition()
        );
    }
}

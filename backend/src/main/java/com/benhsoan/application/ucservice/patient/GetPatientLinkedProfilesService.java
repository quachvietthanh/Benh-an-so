package com.benhsoan.application.ucservice.patient;

import java.util.ArrayList;
import java.util.Comparator;
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
 *
 * <p>QTN-33: dependent discovery only returns valid profiles (active, not merged, and not
 * deactivated). A merged or deactivated dependent must not be selectable for new booking; its
 * historical appointment records are unaffected because they are read through other endpoints.</p>
 *
 * <p>The result is bounded: both the self profile and the dependent lookup are limited to
 * {@link #MAX_LINKED_PROFILES} rows, ordered by full name with a deterministic patientId
 * tie-breaker, so the endpoint never returns an unbounded database result set.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientLinkedProfilesService implements GetPatientLinkedProfilesUseCase {

    static final String SELF_RELATIONSHIP = "SELF";

    /**
     * Maximum number of dependent profiles returned. This is a bounded-list endpoint (a portal
     * account does not need hundreds of dependents) rather than a paginated one, matching the
     * project's other bounded portal list conventions.
     */
    public static final int MAX_LINKED_PROFILES = 50;

    private final PatientRepository patientRepository;

    private final CurrentUserPort currentUserPort;

    @Override
    public List<LinkedPatientProfileResult> getLinkedProfiles() {
        UUID userId = currentUserPort.getCurrentUserId();

        List<Patient> linked = new ArrayList<>();

        patientRepository.findByUserId(userId)
                .filter(Patient::isActive)
                .filter(patient -> !patient.isMerged())
                .ifPresent(linked::add);

        linked.addAll(patientRepository.findValidDependentsByGuardianUserId(userId));

        return linked.stream()
                .distinct()
                .sorted(Comparator
                        .comparing(Patient::getFullName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(Patient::getId))
                .limit(MAX_LINKED_PROFILES)
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

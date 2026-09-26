package com.benhsoan.application.ucservice.patient;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Enforces patient data-scope isolation (NCL-14-CN-002 CV-01 / QTN-23). A logged-in
 * patient may only resolve their own {@link Patient} record; any attempt to access another
 * patient's data is recorded as a denial audit (REQUIRES_NEW) and rejected with HTTP 403.
 *
 * <p>NCL-14-CN-010 CV-02 adds a <em>family scope</em> on top of the own-only scope: the
 * authenticated portal account may additionally reach a dependent patient whose
 * {@code patients.guardian_user_id} points to that account. Authorisation is always derived
 * from the authenticated server-side identity plus database state; a client-supplied
 * {@code patientId} is never accepted as proof of ownership.</p>
 */
@Component
@RequiredArgsConstructor
public class PatientAccessGuard {

    private static final String FAMILY_SCOPE_DENIED_MESSAGE =
            "Patient may only access their own data or the data of a patient they are guardian of.";

    private final CurrentUserPort currentUserPort;
    private final PatientRepository patientRepository;
    private final PatientAccessDeniedAuditWriter denialAuditWriter;
    private final ClockPort clockPort;

    public Patient requirePatientOwnership(UUID targetPatientId) {
        return requirePatientOwnership(targetPatientId, ResourceType.PATIENT, targetPatientId);
    }

    public Patient requirePatientOwnership(
            UUID targetPatientId,
            ResourceType resourceType,
            UUID resourceId
    ) {
        UUID userId = currentUserPort.getCurrentUserId();

        Patient own = patientRepository.findByUserId(userId).orElse(null);

        if (own == null || !own.getId().equals(targetPatientId)) {
            denialAuditWriter.writeDenied(userId, targetPatientId, clockPort.now(), resourceType, resourceId);
            throw new AccessDeniedException("Patient may only access their own data.");
        }

        return own;
    }

    /**
     * NCL-14-CN-010 CV-02: family-scope access. Returns the authenticated user's own patient
     * record, or a dependent patient whose {@code guardianUserId} explicitly points to the
     * authenticated user. Anything else is rejected with HTTP 403 plus the standard
     * ACCESS_DENIED audit entry. The denial message is identical whether or not the target
     * patient exists, so the response does not leak patient existence.
     */
    public Patient requirePatientAccess(UUID targetPatientId) {
        return requirePatientAccess(targetPatientId, ResourceType.PATIENT, targetPatientId);
    }

    public Patient requirePatientAccess(
            UUID targetPatientId,
            ResourceType resourceType,
            UUID resourceId
    ) {
        UUID userId = currentUserPort.getCurrentUserId();

        Patient own = patientRepository.findByUserId(userId).orElse(null);

        if (own != null && own.getId().equals(targetPatientId)) {
            return own;
        }

        // Dependent lookup is keyed on the server-side identity only; any client-supplied
        // guardian / owner claim is ignored.
        Patient dependent = patientRepository
                .findByGuardianUserIdAndId(userId, targetPatientId)
                .orElse(null);

        if (dependent != null) {
            return dependent;
        }

        denyPatientAccess(targetPatientId, resourceType, resourceId);
        throw new AccessDeniedException(FAMILY_SCOPE_DENIED_MESSAGE);
    }

    /**
     * Records an ACCESS_DENIED audit entry for a patient outside the caller's family scope.
     * Used by {@link #requirePatientAccess} and when a resolved resource belongs to a
     * different patient than the requested scope (IDOR guard).
     */
    public void denyPatientAccess(
            UUID targetPatientId,
            ResourceType resourceType,
            UUID resourceId
    ) {
        denialAuditWriter.writeDenied(
                currentUserPort.getCurrentUserId(),
                targetPatientId,
                clockPort.now(),
                resourceType,
                resourceId);
    }
}

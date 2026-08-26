package com.benhsoan.application.ucservice.patient;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Enforces patient data-scope isolation (NCL-14-CN-002 CV-01 / QTN-23). A logged-in
 * patient may only resolve their own {@link Patient} record; any attempt to access another
 * patient's data is recorded as a denial audit (REQUIRES_NEW) and rejected with HTTP 403.
 */
@Component
@RequiredArgsConstructor
public class PatientAccessGuard {

    private final CurrentUserPort currentUserPort;
    private final PatientRepository patientRepository;
    private final PatientAccessDeniedAuditWriter denialAuditWriter;
    private final ClockPort clockPort;

    public Patient requirePatientOwnership(UUID targetPatientId) {
        UUID userId = currentUserPort.getCurrentUserId();

        Patient own = patientRepository.findByUserId(userId).orElse(null);

        if (own == null || !own.getId().equals(targetPatientId)) {
            denialAuditWriter.writeDenied(userId, targetPatientId, clockPort.now());
            throw new AccessDeniedException("Patient may only access their own data.");
        }

        return own;
    }
}

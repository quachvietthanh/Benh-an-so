package com.benhsoan.application.ucservice.portal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.inbound.portal.ReviewAdultGuardianLinksUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NCL-14-CN-010 TC-03: periodic sweep that reviews guardian links whose dependent has reached
 * adulthood and notifies the affected accounts.
 *
 * <p>What this does:</p>
 * <ol>
 *   <li>selects active, non-merged dependents that still carry a guardian user link;</li>
 *   <li>uses the canonical {@link Patient#requiresAdultTransition(java.time.LocalDate)} policy
 *       (18-year threshold, Asia/Ho_Chi_Minh) to decide whether a review is required;</li>
 *   <li>asks {@link PatientPortalNotificationCreator} to notify the dependent's own portal
 *       account and the guardian's account, guarded against duplicates.</li>
 * </ol>
 *
 * <p>What this deliberately does NOT do: it never unlinks the guardian. TC-03 states the link is
 * <em>proposed for removal</em>, so removal stays with the existing authorized staff flow
 * ({@code PUT /patients/{patientId}} with {@code PATIENT_UPDATE}, or the explicit
 * {@code transitionToAdult} transition).</p>
 *
 * <p>The sweep is read-only with respect to the guardian link, so it can run repeatedly without
 * side effects beyond the idempotent notification writes.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewAdultGuardianLinksService implements ReviewAdultGuardianLinksUseCase {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PatientRepository patientRepository;
    private final PatientPortalNotificationCreator notificationCreator;
    private final ClockPort clockPort;

    @Override
    public int reviewAdultGuardianLinks() {
        Instant now = clockPort.now();
        LocalDate today = now.atZone(CLINIC_ZONE).toLocalDate();

        List<Patient> dependents = patientRepository.findGuardianLinkedProfiles();
        if (dependents.isEmpty()) {
            return 0;
        }

        int reviewed = 0;
        for (Patient dependent : dependents) {
            if (!dependent.requiresAdultTransition(today)) {
                continue;
            }
            reviewed++;
            try {
                notificationCreator.createGuardianLinkReview(dependent, now);
            } catch (RuntimeException exception) {
                // One failing recipient must not abort the whole sweep.
                log.warn("Could not raise guardian-link review notification for dependent {}: {}",
                        dependent.getId(), exception.getMessage());
            }
        }
        return reviewed;
    }
}

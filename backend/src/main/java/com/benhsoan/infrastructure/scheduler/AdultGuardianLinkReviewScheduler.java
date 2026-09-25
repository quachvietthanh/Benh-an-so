package com.benhsoan.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.benhsoan.port.inbound.portal.ReviewAdultGuardianLinksUseCase;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-010 TC-03: periodic guardian-link review sweep. Disabled unless
 * {@code patient.portal.guardian-review.enabled=true}, mirroring
 * {@link AppointmentReminderScheduler}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "patient.portal.guardian-review.enabled", havingValue = "true")
public class AdultGuardianLinkReviewScheduler {

    private final ReviewAdultGuardianLinksUseCase reviewAdultGuardianLinksUseCase;

    @Scheduled(fixedDelayString = "${patient.portal.guardian-review.scan-interval-ms}")
    public void reviewAdultGuardianLinks() {
        reviewAdultGuardianLinksUseCase.reviewAdultGuardianLinks();
    }
}

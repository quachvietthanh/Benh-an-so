package com.benhsoan.infrastructure.scheduler;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.benhsoan.port.dto.result.personaldata.PersonalDataRequestResult;
import com.benhsoan.port.inbound.personaldata.ReviewPersonalDataRequestDeadlinesUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NCL-15-CN-006 TC-02: periodic review of personal-data-request deadlines.
 * Disabled unless {@code personal-data-request.deadline-check.enabled=true}.
 *
 * <p>The workbook requires an administrator alert about upcoming/overdue requests
 * but does not define a notification channel. This sweep logs the overdue request
 * ids as the alert evidence; wiring an actual delivery channel is an open decision.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "personal-data-request.deadline-check.enabled", havingValue = "true")
public class PersonalDataRequestDeadlineScheduler {

    private final ReviewPersonalDataRequestDeadlinesUseCase reviewUseCase;

    @Scheduled(fixedDelayString = "${personal-data-request.deadline-check.scan-interval-ms}")
    public void reviewDeadlines() {
        List<PersonalDataRequestResult> overdue = reviewUseCase.reviewDueRequests();
        if (overdue.isEmpty()) {
            return;
        }
        log.warn("Found {} overdue personal-data request(s): {}",
                overdue.size(),
                overdue.stream().map(r -> r.id().toString()).toList());
    }
}

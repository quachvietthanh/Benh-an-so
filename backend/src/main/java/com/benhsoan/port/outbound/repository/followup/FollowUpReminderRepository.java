package com.benhsoan.port.outbound.repository.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.port.dto.command.followup.SearchFollowUpRemindersQuery;

public interface FollowUpReminderRepository {

    FollowUpReminder save(FollowUpReminder reminder);

    Optional<FollowUpReminder> findById(UUID id);

    Page<FollowUpReminder> search(SearchFollowUpRemindersQuery query);

    Page<FollowUpReminder> findDue(Instant currentInstant, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}

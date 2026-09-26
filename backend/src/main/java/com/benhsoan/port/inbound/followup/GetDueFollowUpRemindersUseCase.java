package com.benhsoan.port.inbound.followup;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.FollowUpReminderResult;

public interface GetDueFollowUpRemindersUseCase {

    Page<FollowUpReminderResult> getDue(LocalDate fromDate, LocalDate toDate, Pageable pageable);
}

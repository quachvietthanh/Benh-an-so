package com.benhsoan.port.inbound.followup;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.followup.SearchFollowUpRemindersQuery;
import com.benhsoan.port.dto.result.FollowUpReminderResult;

public interface SearchFollowUpRemindersUseCase {

    Page<FollowUpReminderResult> search(SearchFollowUpRemindersQuery query);
}

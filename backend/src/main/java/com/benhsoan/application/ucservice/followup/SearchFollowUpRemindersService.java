package com.benhsoan.application.ucservice.followup;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.followup.SearchFollowUpRemindersQuery;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.inbound.followup.SearchFollowUpRemindersUseCase;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchFollowUpRemindersService implements SearchFollowUpRemindersUseCase {

    private final FollowUpReminderRepository followUpReminderRepository;
    private final FollowUpReminderResultMapper resultMapper;
    private final FollowUpReminderAuthorizer authorizer;

    @Override
    public Page<FollowUpReminderResult> search(SearchFollowUpRemindersQuery query) {
        authorizer.requireReceptionistOrAdmin();

        return followUpReminderRepository.search(query).map(resultMapper::toResult);
    }
}

package com.benhsoan.application.ucservice.followup;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.inbound.followup.GetDueFollowUpRemindersUseCase;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDueFollowUpRemindersService implements GetDueFollowUpRemindersUseCase {

    private final FollowUpReminderRepository followUpReminderRepository;
    private final FollowUpReminderResultMapper resultMapper;
    private final FollowUpReminderAuthorizer authorizer;
    private final ClockPort clockPort;

    @Override
    public Page<FollowUpReminderResult> getDue(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        authorizer.requireReceptionistOrAdmin();

        return followUpReminderRepository.findDue(clockPort.now(), fromDate, toDate, pageable)
                .map(resultMapper::toResult);
    }
}

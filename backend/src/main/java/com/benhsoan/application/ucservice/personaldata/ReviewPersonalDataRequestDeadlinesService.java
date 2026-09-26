package com.benhsoan.application.ucservice.personaldata;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.personaldata.PersonalDataRequestResult;
import com.benhsoan.port.inbound.personaldata.ReviewPersonalDataRequestDeadlinesUseCase;
import com.benhsoan.port.outbound.repository.personaldata.PersonalDataRequestRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-15-CN-006 TC-02: periodic review of processing deadlines. Returns the open
 * requests that are already past their due date. A completed request is never
 * reported as overdue. Delivery of the administrator alert is left to the caller
 * (scheduler logs it; no external notification channel is invented here).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewPersonalDataRequestDeadlinesService implements ReviewPersonalDataRequestDeadlinesUseCase {

    private final PersonalDataRequestRepository requestRepository;
    private final PersonalDataRequestResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public List<PersonalDataRequestResult> reviewDueRequests() {
        return requestRepository.findOpenWithDueBefore(clockPort.now()).stream()
                .map(resultMapper::toResult)
                .toList();
    }
}

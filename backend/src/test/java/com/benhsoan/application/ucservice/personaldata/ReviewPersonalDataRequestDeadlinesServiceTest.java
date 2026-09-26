package com.benhsoan.application.ucservice.personaldata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.personaldata.PersonalDataRequest;
import com.benhsoan.domain.personaldata.enums.PersonalDataRequestStatus;
import com.benhsoan.port.dto.result.personaldata.PersonalDataRequestResult;
import com.benhsoan.port.outbound.repository.personaldata.PersonalDataRequestRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ReviewPersonalDataRequestDeadlinesServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-26T08:00:00Z");

    @Mock private PersonalDataRequestRepository requestRepository;
    @Mock private PersonalDataRequestResultMapper resultMapper;
    @Mock private ClockPort clockPort;

    @InjectMocks
    private ReviewPersonalDataRequestDeadlinesService service;

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    @DisplayName("TC-02: trả về các yêu cầu RECEIVED đã quá hạn")
    void reviewReturnsOpenOverdueRequests() {
        PersonalDataRequest overdue = PersonalDataRequest.create(
                UUID.randomUUID(), PersonalDataRequest.TYPE_MEDICAL_RECORD_COPY, null, NOW, NOW.minusSeconds(1));
        when(requestRepository.findOpenWithDueBefore(NOW)).thenReturn(List.of(overdue));
        when(resultMapper.toResult(overdue)).thenReturn(resultFor(overdue));

        List<PersonalDataRequestResult> results = service.reviewDueRequests();

        assertEquals(1, results.size());
        assertTrue(results.get(0).overdue());
    }

    private PersonalDataRequestResult resultFor(PersonalDataRequest r) {
        return new PersonalDataRequestResult(
                r.getId(), r.getPatientId(), r.getRequestType(), r.getStatus(), r.getReason(),
                r.getReceivedAt(), r.getDueAt(), r.getResult(), r.getCompletedAt(), r.getProcessedBy(),
                r.getCreatedAt(), r.getUpdatedAt(), r.isOverdue(NOW));
    }
}

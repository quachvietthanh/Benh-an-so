package com.benhsoan.application.ucservice.personaldata;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.personaldata.PersonalDataRequest;
import com.benhsoan.domain.personaldata.enums.PersonalDataRequestStatus;
import com.benhsoan.domain.personaldata.exception.PersonalDataRequestNotFoundException;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.personaldata.PersonalDataRequestRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class CompletePersonalDataRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-26T08:00:00Z");

    @Mock private PersonalDataRequestRepository requestRepository;
    @Mock private PersonalDataRequestAuthorizer authorizer;
    @Mock private PersonalDataRequestResultMapper resultMapper;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    @InjectMocks
    private CompletePersonalDataRequestService service;

    @Test
    @DisplayName("TC-03: hoàn tất yêu cầu ghi kết quả, người xử lý và audit")
    void completeRecordsResultProcessorAndAudit() {
        UUID requestId = UUID.randomUUID();
        UUID processorId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(processorId);

        PersonalDataRequest request = PersonalDataRequest.create(
                UUID.randomUUID(), PersonalDataRequest.TYPE_MEDICAL_RECORD_COPY, null, NOW, NOW.plusSeconds(86400));
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.complete(requestId, "Đã cấp bản sao");

        verify(authorizer).requireUpdatePermission();
        verify(requestRepository).save(argThat(r ->
                r.getStatus() == PersonalDataRequestStatus.COMPLETED
                        && r.getResult().equals("Đã cấp bản sao")
                        && r.getProcessedBy().equals(processorId)));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("yêu cầu không tồn tại bị từ chối")
    void completeRejectsUnknownRequest() {
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(PersonalDataRequestNotFoundException.class,
                () -> service.complete(requestId, "Đã xử lý"));
    }
}

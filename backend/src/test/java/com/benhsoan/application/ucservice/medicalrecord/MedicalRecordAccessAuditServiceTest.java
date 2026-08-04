package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;

@ExtendWith(MockitoExtension.class)
class MedicalRecordAccessAuditServiceTest {

    @Mock
    private MedicalRecordAccessLogRepository medicalRecordAccessLogRepository;
    @InjectMocks
    private MedicalRecordAccessAuditService service;

    @Test
    void recordsHistoryViewWithConfiguredTimestamp() {
        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant accessedAt = Instant.parse("2026-08-20T02:00:00Z");
        when(medicalRecordAccessLogRepository.save(any(MedicalRecordAccessLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordHistoryView(patientId, userId, accessedAt);

        ArgumentCaptor<MedicalRecordAccessLog> logCaptor = ArgumentCaptor.forClass(MedicalRecordAccessLog.class);
        verify(medicalRecordAccessLogRepository).save(logCaptor.capture());
        assertEquals(MedicalRecordAccessAction.VIEW_HISTORY, logCaptor.getValue().getAction());
        assertEquals(accessedAt, logCaptor.getValue().getAccessedAt());
        assertNull(logCaptor.getValue().getIpAddress());
    }
}

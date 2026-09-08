package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.appointment.exception.DoctorTimeOffNotFoundException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CancelDoctorTimeOffServiceTest {

    private static final UUID TIME_OFF_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-08T10:00:00Z");

    @Mock private DoctorTimeOffRepository doctorTimeOffRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CancelDoctorTimeOffService service;

    @BeforeEach
    void setUp() {
        service = new CancelDoctorTimeOffService(
                doctorTimeOffRepository,
                currentUserPort,
                auditLogRepository,
                clockPort,
                objectMapper
        );
    }

    @Test
    void cancelTimeOff_Success() {
        DoctorTimeOff timeOff = DoctorTimeOff.restore(
                TIME_OFF_ID, DOCTOR_ID, NOW.plusSeconds(3600), NOW.plusSeconds(7200),
                "Nghỉ phép", TimeOffStatus.ACTIVE, ACTOR_ID, NOW, null
        );

        when(doctorTimeOffRepository.findById(TIME_OFF_ID)).thenReturn(Optional.of(timeOff));
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(doctorTimeOffRepository.save(any(DoctorTimeOff.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorTimeOffResult result = service.cancelTimeOff(TIME_OFF_ID);

        assertNotNull(result);
        assertEquals(TimeOffStatus.CANCELLED, result.status());
        assertEquals(TIME_OFF_ID, result.id());

        verify(doctorTimeOffRepository).save(timeOff);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ActionType.DELETE, audit.getActionType());
        assertEquals(ResourceType.DOCTOR_TIME_OFF, audit.getResourceType());
        assertEquals(TIME_OFF_ID, audit.getResourceId());
    }

    @Test
    void cancelTimeOff_AlreadyCancelled_ReturnsImmediatelyWithoutSavingOrAudit() {
        DoctorTimeOff timeOff = DoctorTimeOff.restore(
                TIME_OFF_ID, DOCTOR_ID, NOW.plusSeconds(3600), NOW.plusSeconds(7200),
                "Nghỉ phép", TimeOffStatus.CANCELLED, ACTOR_ID, NOW, NOW
        );

        when(doctorTimeOffRepository.findById(TIME_OFF_ID)).thenReturn(Optional.of(timeOff));

        DoctorTimeOffResult result = service.cancelTimeOff(TIME_OFF_ID);

        assertNotNull(result);
        assertEquals(TimeOffStatus.CANCELLED, result.status());

        // FINDING-08: idempotent cancellation should not re-save or duplicate audit log
        verify(doctorTimeOffRepository, never()).save(any(DoctorTimeOff.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void cancelTimeOff_NotFound_ThrowsException() {
        when(doctorTimeOffRepository.findById(TIME_OFF_ID)).thenReturn(Optional.empty());

        assertThrows(DoctorTimeOffNotFoundException.class, () -> service.cancelTimeOff(TIME_OFF_ID));
    }
}

package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.DoctorTimeOffNotFoundException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class CancelDoctorTimeOffServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID TIME_OFF_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock private DoctorTimeOffRepository doctorTimeOffRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    private CancelDoctorTimeOffService service;

    @BeforeEach
    void setUp() {
        service = new CancelDoctorTimeOffService(
                doctorTimeOffRepository,
                userRepository,
                currentUserPort,
                auditLogRepository,
                clockPort,
                objectMapper
        );
    }

    @Test
    void cancelsTimeOffSuccessfullyAndAudits() {
        User doctor = User.restore(DOCTOR_ID, "dr_test", "hash", "Dr. Test", "dr@test.com", "0901234567",
                UUID.randomUUID(), true, null, NOW);
        DoctorTimeOff timeOff = DoctorTimeOff.restore(
                TIME_OFF_ID, DOCTOR_ID, NOW.plusSeconds(3600), NOW.plusSeconds(7200),
                "Di cong tac", TimeOffStatus.ACTIVE, ACTOR_ID, NOW, null);

        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctorTimeOffRepository.findById(TIME_OFF_ID)).thenReturn(Optional.of(timeOff));
        when(clockPort.now()).thenReturn(NOW);
        when(doctorTimeOffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);

        DoctorTimeOffResult result = service.cancelTimeOff(DOCTOR_ID, TIME_OFF_ID);

        assertEquals(TimeOffStatus.CANCELLED, result.status());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ActionType.CANCEL, audit.getActionType());
        assertEquals(ResourceType.DOCTOR_TIMEOFF, audit.getResourceType());
        assertEquals(TIME_OFF_ID, audit.getResourceId());
        assertEquals("{\"status\":\"CANCELLED\",\"cancelledAt\":\"" + NOW + "\"}", audit.getDetail());
    }

    @Test
    void rejectsWhenDoctorNotFound() {
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThrows(DoctorNotFoundException.class, () -> service.cancelTimeOff(DOCTOR_ID, TIME_OFF_ID));
    }

    @Test
    void rejectsWhenTimeOffNotFound() {
        User doctor = User.restore(DOCTOR_ID, "dr_test", "hash", "Dr. Test", "dr@test.com", "0901234567",
                UUID.randomUUID(), true, null, NOW);
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctorTimeOffRepository.findById(TIME_OFF_ID)).thenReturn(Optional.empty());

        assertThrows(DoctorTimeOffNotFoundException.class, () -> service.cancelTimeOff(DOCTOR_ID, TIME_OFF_ID));
    }

    @Test
    void rejectsWhenTimeOffBelongsToDifferentDoctor() {
        UUID otherDoctorId = UUID.randomUUID();
        User doctor = User.restore(DOCTOR_ID, "dr_test", "hash", "Dr. Test", "dr@test.com", "0901234567",
                UUID.randomUUID(), true, null, NOW);
        DoctorTimeOff timeOff = DoctorTimeOff.restore(
                TIME_OFF_ID, otherDoctorId, NOW.plusSeconds(3600), NOW.plusSeconds(7200),
                "Di cong tac", TimeOffStatus.ACTIVE, ACTOR_ID, NOW, null);

        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctorTimeOffRepository.findById(TIME_OFF_ID)).thenReturn(Optional.of(timeOff));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.cancelTimeOff(DOCTOR_ID, TIME_OFF_ID));
        assertEquals("Khoảng nghỉ không thuộc về bác sĩ này.", ex.getMessage());
    }
}

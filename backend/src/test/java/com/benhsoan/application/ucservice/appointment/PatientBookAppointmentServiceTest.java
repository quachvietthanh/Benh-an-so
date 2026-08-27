package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.InvalidAppointmentTimeException;
import com.benhsoan.domain.appointment.exception.SlotAlreadyBookedException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.command.appointment.PatientBookAppointmentCommand;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PatientBookAppointmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");
    private static final LocalDate FUTURE_DATE = LocalDate.of(2099, 8, 10);
    private static final LocalDate PAST_DATE = LocalDate.of(2026, 8, 25);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentCodeGenerator appointmentCodeGenerator;
    @Mock private DoctorScheduleRepository doctorScheduleRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PatientBookAppointmentService service;

    @BeforeEach
    void setUp() {
        service = new PatientBookAppointmentService(
                appointmentRepository,
                appointmentCodeGenerator,
                doctorScheduleRepository,
                patientRepository,
                userRepository,
                currentUserPort,
                auditLogRepository,
                clockPort,
                objectMapper
        );
    }

    @Test
    void booksAppointmentWithOnlinePortalChannelAndWritesAuditLog() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);

        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com",
                "0900000001", roleId, true, null, Instant.parse("2026-01-01T00:00:00Z"));

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.empty());
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(
                eq(doctorId), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(appointmentCodeGenerator.generate()).thenReturn("APT000100");
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientAppointmentResult result = service.book(
                new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, START_TIME, "Đau đầu"));

        assertEquals("ONLINE_PORTAL", result.bookingChannel());
        assertEquals(AppointmentStatus.SCHEDULED, result.status());
        assertEquals(patientId, result.patientId());
        assertEquals("APT000100", result.appointmentCode());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(ActionType.CREATE, log.getActionType());
        assertEquals(ResourceType.APPOINTMENT, log.getResourceType());

        JsonNode node = objectMapper.readTree(log.getDetail());
        assertEquals(patientId.toString(), node.get("patientId").asText());
        assertEquals(doctorId.toString(), node.get("doctorId").asText());
        assertEquals("ONLINE_PORTAL", node.get("channel").asText());
        assertEquals(NOW.toString(), node.get("bookedAt").asText());
    }

    @Test
    void rejectsSlotCollisionWithConflict() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);

        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com",
                "0900000001", roleId, true, null, Instant.parse("2026-01-01T00:00:00Z"));

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.empty());

        Appointment existing = mock(Appointment.class);
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(
                eq(doctorId), any(Instant.class), any(Instant.class))).thenReturn(List.of(existing));

        assertThrows(SlotAlreadyBookedException.class,
                () -> service.book(new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, START_TIME, null)));

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void rejectsPastAppointmentTimeWithBadRequest() {
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(InvalidAppointmentTimeException.class,
                () -> service.book(new PatientBookAppointmentCommand(
                        UUID.randomUUID(), PAST_DATE, START_TIME, null)));
    }
}

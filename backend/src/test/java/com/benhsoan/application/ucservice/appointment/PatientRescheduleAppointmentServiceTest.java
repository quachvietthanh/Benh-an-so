package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentPastCutoffException;
import com.benhsoan.domain.appointment.exception.InvalidAppointmentTimeException;
import com.benhsoan.domain.appointment.exception.SlotAlreadyBookedException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.command.appointment.PatientRescheduleAppointmentCommand;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PatientRescheduleAppointmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");
    private static final Instant OLD_START = Instant.parse("2099-08-10T02:00:00Z");
    private static final Instant OLD_END = Instant.parse("2099-08-10T02:30:00Z");
    private static final LocalDate NEW_DATE = LocalDate.of(2099, 8, 11);
    private static final LocalTime NEW_TIME = LocalTime.of(10, 0);
    private static final Instant NEW_START = ZonedDateTime.of(NEW_DATE, NEW_TIME,
            ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
    private static final Instant NEW_END = NEW_START.plusSeconds(1800);

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DoctorScheduleRepository doctorScheduleRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private PatientAccessGuard patientAccessGuard;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;

    private PatientRescheduleAppointmentService service;

    @BeforeEach
    void setUp() {
        service = new PatientRescheduleAppointmentService(
                appointmentRepository,
                doctorScheduleRepository,
                userRepository,
                currentUserPort,
                patientAccessGuard,
                auditLogRepository,
                clockPort,
                new PatientAppointmentResultMapper(),
                new ObjectMapper()
        );
    }

    @Test
    void reschedulesFutureAppointmentAndWritesAudit() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        Appointment appointment = Appointment.restore(appointmentId, "AP000400", patientId, doctorId,
                OLD_START, OLD_END, AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, actorId, Instant.parse("2026-08-01T00:00:00Z"));

        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com",
                "0900000001", roleId, true, null, Instant.parse("2026-08-01T00:00:00Z"));
        DoctorSchedule schedule = DoctorSchedule.create(doctorId, NEW_DATE, LocalTime.of(8, 0), LocalTime.of(17, 0));

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId)).thenReturn(mock(Patient.class));
        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, NEW_DATE))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(doctorId, NEW_START, NEW_END))
                .thenReturn(List.of());
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        service.reschedule(appointmentId, new PatientRescheduleAppointmentCommand(NEW_DATE, NEW_TIME, "Đổi lịch"));

        assertEquals(NEW_START, appointment.getStartTime());
        assertEquals(NEW_END, appointment.getEndTime());
        assertEquals("Đổi lịch", appointment.getReason());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.UPDATE, audit.getActionType());
        assertEquals(ResourceType.APPOINTMENT, audit.getResourceType());
        assertEquals(appointmentId, audit.getResourceId());
    }

    @Test
    void rejectsPastAppointmentWithPastCutoff() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Instant pastStart = Instant.parse("2026-08-25T09:00:00Z");
        Appointment appointment = Appointment.restore(appointmentId, "AP000401", patientId, doctorId,
                pastStart, pastStart.plusSeconds(1800), AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, actorId, Instant.parse("2026-08-01T00:00:00Z"));

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId)).thenReturn(mock(Patient.class));
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(AppointmentPastCutoffException.class,
                () -> service.reschedule(appointmentId,
                        new PatientRescheduleAppointmentCommand(NEW_DATE, NEW_TIME, "Đổi lịch")));
    }

    @Test
    void rejectsCrossPatientAccessWithForbidden() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Appointment appointment = Appointment.restore(appointmentId, "AP000402", patientId, doctorId,
                OLD_START, OLD_END, AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, actorId, Instant.parse("2026-08-01T00:00:00Z"));

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        assertThrows(AccessDeniedException.class,
                () -> service.reschedule(appointmentId,
                        new PatientRescheduleAppointmentCommand(NEW_DATE, NEW_TIME, "Đổi lịch")));
    }

    @Test
    void rejectsUnalignedSlot() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Appointment appointment = Appointment.restore(appointmentId, "AP000403", patientId, doctorId,
                OLD_START, OLD_END, AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, actorId, Instant.parse("2026-08-01T00:00:00Z"));

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId)).thenReturn(mock(Patient.class));
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(InvalidAppointmentTimeException.class,
                () -> service.reschedule(appointmentId,
                        new PatientRescheduleAppointmentCommand(NEW_DATE, LocalTime.of(10, 17), "Đổi lịch")));
    }

    @Test
    void rejectsSlotConflict() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        Appointment appointment = Appointment.restore(appointmentId, "AP000404", patientId, doctorId,
                OLD_START, OLD_END, AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, actorId, Instant.parse("2026-08-01T00:00:00Z"));
        Appointment other = Appointment.restore(UUID.randomUUID(), "AP000405", patientId, doctorId,
                NEW_START, NEW_END, AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, actorId, Instant.parse("2026-08-01T00:00:00Z"));

        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com",
                "0900000001", roleId, true, null, Instant.parse("2026-08-01T00:00:00Z"));
        DoctorSchedule schedule = DoctorSchedule.create(doctorId, NEW_DATE, LocalTime.of(8, 0), LocalTime.of(17, 0));

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId)).thenReturn(mock(Patient.class));
        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, NEW_DATE))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(doctorId, NEW_START, NEW_END))
                .thenReturn(List.of(other));

        assertThrows(SlotAlreadyBookedException.class,
                () -> service.reschedule(appointmentId,
                        new PatientRescheduleAppointmentCommand(NEW_DATE, NEW_TIME, "Đổi lịch")));
    }
}

package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentTimeConflictException;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotWorkingException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class CreateAppointmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T08:00:00Z");
    private static final Instant APPOINTMENT_START = Instant.parse("2099-08-10T09:00:00Z");
    private static final Instant APPOINTMENT_END = Instant.parse("2099-08-10T09:30:00Z");

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private AppointmentCodeGenerator appointmentCodeGenerator;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private DoctorScheduleValidator doctorScheduleValidator;
    @Mock private ClockPort clockPort;

    private CreateAppointmentService service;

    @BeforeEach
    void setUp() {
        service = new CreateAppointmentService(
                appointmentRepository,
                patientRepository,
                userRepository,
                appointmentCodeGenerator,
                currentUserPort,
                new AppointmentResultMapper(),
                auditLogRepository,
                doctorScheduleValidator,
                clockPort
        );
    }

    @Test
    void createsAppointmentAndWritesAuditLog() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        Patient patient = mock(Patient.class);
        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com", "0900000001",
                UUID.randomUUID(), true, null, Instant.parse("2026-08-01T00:00:00Z"));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(appointmentCodeGenerator.generate()).thenReturn("APT000100");
        when(appointmentRepository.existsActiveAppointmentConflict(doctorId, APPOINTMENT_START, APPOINTMENT_END)).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResult result = service.create(new CreateAppointmentCommand(
                patientId, doctorId, APPOINTMENT_START, APPOINTMENT_END, "Tai kham tong quat"
        ));

        assertEquals("APT000100", result.appointmentCode());
        assertEquals(patientId, result.patientId());
        assertEquals(doctorId, result.doctorId());
        assertEquals("Tai kham tong quat", result.reason());
        verify(appointmentRepository).save(any(Appointment.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void rejectsInactiveDoctor() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        Patient patient = mock(Patient.class);
        User inactiveDoctor = User.restore(doctorId, "doctor2", "hash", "Doctor Two", "doctor2@example.com",
                "0900000002", UUID.randomUUID(), false, null, Instant.parse("2026-08-01T00:00:00Z"));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(inactiveDoctor));

        assertThrows(DoctorInactiveException.class, () -> service.create(new CreateAppointmentCommand(
                patientId, doctorId, APPOINTMENT_START, APPOINTMENT_END, "Tai kham tong quat"
        )));
    }

    @Test
    void rejectsDoctorScheduleConflict() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        Patient patient = mock(Patient.class);
        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com", "0900000001",
                UUID.randomUUID(), true, null, Instant.parse("2026-08-01T00:00:00Z"));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(appointmentRepository.existsActiveAppointmentConflict(doctorId, APPOINTMENT_START, APPOINTMENT_END)).thenReturn(true);

        assertThrows(AppointmentTimeConflictException.class, () -> service.create(new CreateAppointmentCommand(
                patientId, doctorId, APPOINTMENT_START, APPOINTMENT_END, "Tai kham tong quat"
        )));
    }

    @Test
    void rejectsWhenDoctorNotWorkingOrInTimeOff() {
        // TC-02 / QTN-30: Bác sĩ không làm việc trong khung giờ này
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        Patient patient = mock(Patient.class);
        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com", "0900000001",
                UUID.randomUUID(), true, null, Instant.parse("2026-08-01T00:00:00Z"));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        org.mockito.Mockito.doThrow(new DoctorNotWorkingException("Bác sĩ không làm việc trong khung giờ này."))
                .when(doctorScheduleValidator).validateDoctorWorkingAndAvailable(doctorId, APPOINTMENT_START, APPOINTMENT_END);

        DoctorNotWorkingException ex = assertThrows(
                DoctorNotWorkingException.class,
                () -> service.create(new CreateAppointmentCommand(patientId, doctorId, APPOINTMENT_START, APPOINTMENT_END, "Tai kham"))
        );
        assertEquals("Bác sĩ không làm việc trong khung giờ này.", ex.getMessage());
    }

    @Test
    void rejectsWhenStartTimeInThePastWithCorrectMessage() {
        // Finding [P2-1]: Kiểm tra startTime ở quá khứ sử dụng ClockPort và trả về thông điệp chính xác
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Instant pastStartTime = NOW.minusSeconds(1);
        Instant futureEndTime = NOW.plusSeconds(1800);

        when(clockPort.now()).thenReturn(NOW);
        Patient patient = mock(Patient.class);
        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com", "0900000001",
                UUID.randomUUID(), true, null, Instant.parse("2026-08-01T00:00:00Z"));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(new CreateAppointmentCommand(patientId, doctorId, pastStartTime, futureEndTime, "Tai kham"))
        );
        assertEquals("Thời gian đặt lịch không được ở trong quá khứ.", ex.getMessage());
    }

    @Test
    void rejectsWhenEndTimeBeforeStartTimeWithCorrectMessage() {
        // Finding [P2-1]: Kiểm tra endTime trước startTime trả về đúng thông điệp
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Instant futureStartTime = NOW.plusSeconds(3600);
        Instant invalidEndTime = NOW.plusSeconds(1800); // trước startTime

        Patient patient = mock(Patient.class);
        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com", "0900000001",
                UUID.randomUUID(), true, null, Instant.parse("2026-08-01T00:00:00Z"));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(new CreateAppointmentCommand(patientId, doctorId, futureStartTime, invalidEndTime, "Tai kham"))
        );
        assertEquals("Appointment end time must be after start time.", ex.getMessage());
    }
}

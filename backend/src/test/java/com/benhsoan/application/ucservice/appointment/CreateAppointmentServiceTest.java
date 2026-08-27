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

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentTimeConflictException;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class CreateAppointmentServiceTest {

    private static final Instant APPOINTMENT_START = Instant.parse("2099-08-10T09:00:00Z");
    private static final Instant APPOINTMENT_END = Instant.parse("2099-08-10T09:30:00Z");

    @Test
    void createsAppointmentAndWritesAuditLog() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant startTime = APPOINTMENT_START;
        Instant endTime = APPOINTMENT_END;
        PatientRepository patientRepository = mock(PatientRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        AppointmentCodeGenerator appointmentCodeGenerator = mock(AppointmentCodeGenerator.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        CreateAppointmentService service = new CreateAppointmentService(
                appointmentRepository,
                patientRepository,
                userRepository,
                appointmentCodeGenerator,
                currentUserPort,
                new AppointmentResultMapper(),
                auditLogRepository
        );

        Patient patient = mock(Patient.class);
        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com", "0900000001",
                UUID.randomUUID(), true, null, Instant.parse("2026-08-01T00:00:00Z"));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(appointmentCodeGenerator.generate()).thenReturn("APT000100");
        when(appointmentRepository.existsActiveAppointmentConflict(doctorId, startTime, endTime)).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResult result = service.create(new CreateAppointmentCommand(
                patientId, doctorId, startTime, endTime, "Tai kham tong quat"
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
        Instant startTime = APPOINTMENT_START;
        Instant endTime = APPOINTMENT_END;
        PatientRepository patientRepository = mock(PatientRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        AppointmentCodeGenerator appointmentCodeGenerator = mock(AppointmentCodeGenerator.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        CreateAppointmentService service = new CreateAppointmentService(
                appointmentRepository,
                patientRepository,
                userRepository,
                appointmentCodeGenerator,
                currentUserPort,
                new AppointmentResultMapper(),
                auditLogRepository
        );

        Patient patient = mock(Patient.class);
        User inactiveDoctor = User.restore(doctorId, "doctor2", "hash", "Doctor Two", "doctor2@example.com",
                "0900000002", UUID.randomUUID(), false, null, Instant.parse("2026-08-01T00:00:00Z"));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(inactiveDoctor));

        assertThrows(DoctorInactiveException.class, () -> service.create(new CreateAppointmentCommand(
                patientId, doctorId, startTime, endTime, "Tai kham tong quat"
        )));
    }

    @Test
    void rejectsDoctorScheduleConflict() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Instant startTime = APPOINTMENT_START;
        Instant endTime = APPOINTMENT_END;
        PatientRepository patientRepository = mock(PatientRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        AppointmentCodeGenerator appointmentCodeGenerator = mock(AppointmentCodeGenerator.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        CreateAppointmentService service = new CreateAppointmentService(
                appointmentRepository,
                patientRepository,
                userRepository,
                appointmentCodeGenerator,
                currentUserPort,
                new AppointmentResultMapper(),
                auditLogRepository
        );

        Patient patient = mock(Patient.class);
        User doctor = User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com", "0900000001",
                UUID.randomUUID(), true, null, Instant.parse("2026-08-01T00:00:00Z"));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(appointmentRepository.existsActiveAppointmentConflict(doctorId, startTime, endTime)).thenReturn(true);

        assertThrows(AppointmentTimeConflictException.class, () -> service.create(new CreateAppointmentCommand(
                patientId, doctorId, startTime, endTime, "Tai kham tong quat"
        )));
    }
}

package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentSeries;
import com.benhsoan.domain.appointment.AppointmentSeriesConflictDetail;
import com.benhsoan.domain.appointment.enums.AppointmentSeriesStatus;
import com.benhsoan.domain.appointment.exception.AppointmentSeriesConflictException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentSeriesCommand;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentSeriesCommand.AppointmentSeriesSessionCommand;
import com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.generator.AppointmentSeriesCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentSeriesRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class CreateAppointmentSeriesServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");

    @Mock private AppointmentSeriesRepository appointmentSeriesRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private AppointmentCodeGenerator appointmentCodeGenerator;
    @Mock private AppointmentSeriesCodeGenerator appointmentSeriesCodeGenerator;
    @Mock private AppointmentSeriesValidator appointmentSeriesValidator;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AppointmentAccessDeniedAuditWriter appointmentAccessDeniedAuditWriter;

    private CreateAppointmentSeriesService service;

    @BeforeEach
    void setUp() {
        service = new CreateAppointmentSeriesService(
                appointmentSeriesRepository,
                appointmentRepository,
                patientRepository,
                userRepository,
                medicalRecordRepository,
                visitRepository,
                appointmentCodeGenerator,
                appointmentSeriesCodeGenerator,
                appointmentSeriesValidator,
                new AppointmentSeriesResultMapper(),
                new AppointmentResultMapper(),
                currentUserPort,
                clockPort,
                auditLogRepository,
                appointmentAccessDeniedAuditWriter
        );
    }

    @Test
    void createsAppointmentSeriesSuccessfully() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW);

        Patient patient = mock(Patient.class);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        User doctor = User.restore(doctorId, "doctor1", "hash", "Dr. A", "a@example.com", "0900000001",
                UUID.randomUUID(), true, null, NOW);
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));

        when(appointmentSeriesValidator.validateSessions(eq(doctorId), any())).thenReturn(List.of());
        when(appointmentSeriesCodeGenerator.generate()).thenReturn("SER000001");
        when(appointmentCodeGenerator.generateBatch(2)).thenReturn(List.of("APT000001", "APT000002"));

        when(appointmentSeriesRepository.save(any(AppointmentSeries.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var sessions = List.of(
                new AppointmentSeriesSessionCommand(1, NOW.plusSeconds(3600), NOW.plusSeconds(5400)),
                new AppointmentSeriesSessionCommand(2, NOW.plusSeconds(86400 * 7), NOW.plusSeconds(86400 * 7 + 1800))
        );

        CreateAppointmentSeriesCommand command = CreateAppointmentSeriesCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .title("Lieu trinh vat ly tri lieu")
                .notes("Tap co vai gay")
                .totalSessions(2)
                .intervalDays(7)
                .sessions(sessions)
                .build();

        AppointmentSeriesResult result = service.create(command);

        assertNotNull(result);
        assertEquals("SER000001", result.seriesCode());
        assertEquals(AppointmentSeriesStatus.ACTIVE, result.status());
        assertEquals(2, result.totalSessions());
        assertEquals(2, result.appointments().size());
        assertEquals("APT000001", result.appointments().get(0).appointmentCode());
        assertEquals("APT000002", result.appointments().get(1).appointmentCode());

        verify(appointmentSeriesRepository).save(any(AppointmentSeries.class));
        verify(appointmentRepository, times(2)).save(any(Appointment.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void rejectsDoctorOrUnauthorizedUserAndWritesAuditLog() {
        UUID actorId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(clockPort.now()).thenReturn(NOW);

        CreateAppointmentSeriesCommand command = CreateAppointmentSeriesCommand.builder()
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .totalSessions(2)
                .intervalDays(7)
                .sessions(List.of(
                        new AppointmentSeriesSessionCommand(1, NOW.plusSeconds(3600), NOW.plusSeconds(5400)),
                        new AppointmentSeriesSessionCommand(2, NOW.plusSeconds(7200), NOW.plusSeconds(9000))
                ))
                .build();

        assertThrows(UnauthorizedAppointmentOperationException.class, () -> service.create(command));
        verify(appointmentAccessDeniedAuditWriter).writeSeriesCreateDenied(eq(actorId), eq(NOW), any());
        verify(appointmentSeriesRepository, never()).save(any());
    }

    @Test
    void rejectsWhenMedicalRecordBelongsToDifferentPatient() {
        UUID patientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        Patient patient = mock(Patient.class);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        User doctor = User.restore(doctorId, "doctor1", "hash", "Dr. A", "a@example.com", "0900000001",
                UUID.randomUUID(), true, null, NOW);
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));

        MedicalRecord record = mock(MedicalRecord.class);
        when(record.getVisitId()).thenReturn(visitId);
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        Visit visit = mock(Visit.class);
        when(visit.getPatientId()).thenReturn(otherPatientId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        CreateAppointmentSeriesCommand command = CreateAppointmentSeriesCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .medicalRecordId(recordId)
                .totalSessions(2)
                .intervalDays(7)
                .sessions(List.of(
                        new AppointmentSeriesSessionCommand(1, NOW.plusSeconds(3600), NOW.plusSeconds(5400)),
                        new AppointmentSeriesSessionCommand(2, NOW.plusSeconds(7200), NOW.plusSeconds(9000))
                ))
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.create(command));
        assertEquals("Bệnh án không thuộc về bệnh nhân này.", ex.getMessage());
        verify(appointmentSeriesRepository, never()).save(any());
    }

    @Test
    void rejectsWhenSessionCountMismatchesTotalSessions() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        Patient patient = mock(Patient.class);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        User doctor = User.restore(doctorId, "doctor1", "hash", "Dr. A", "a@example.com", "0900000001",
                UUID.randomUUID(), true, null, NOW);
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));

        CreateAppointmentSeriesCommand command = CreateAppointmentSeriesCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .totalSessions(3)
                .intervalDays(7)
                .sessions(List.of(
                        new AppointmentSeriesSessionCommand(1, NOW.plusSeconds(3600), NOW.plusSeconds(5400))
                ))
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.create(command));
        assertEquals("Số lượng buổi trong danh sách (1) không khớp với tổng số buổi (3).", ex.getMessage());
    }

    @Test
    void rejectsWhenValidatorDetectsConflictsAndDoesNotSaveAnything() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        Patient patient = mock(Patient.class);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        User doctor = User.restore(doctorId, "doctor1", "hash", "Dr. A", "a@example.com", "0900000001",
                UUID.randomUUID(), true, null, NOW);
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));

        var conflictDetail = new AppointmentSeriesConflictDetail(
                2, NOW.plusSeconds(7200), NOW.plusSeconds(9000), "DOCTOR_TIME_OFF", "Bác sĩ nghỉ phép."
        );
        when(appointmentSeriesValidator.validateSessions(eq(doctorId), any()))
                .thenReturn(List.of(conflictDetail));

        CreateAppointmentSeriesCommand command = CreateAppointmentSeriesCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .totalSessions(2)
                .intervalDays(7)
                .sessions(List.of(
                        new AppointmentSeriesSessionCommand(1, NOW.plusSeconds(3600), NOW.plusSeconds(5400)),
                        new AppointmentSeriesSessionCommand(2, NOW.plusSeconds(7200), NOW.plusSeconds(9000))
                ))
                .build();

        AppointmentSeriesConflictException ex = assertThrows(
                AppointmentSeriesConflictException.class,
                () -> service.create(command)
        );
        assertEquals(1, ex.getConflicts().size());
        assertEquals("DOCTOR_TIME_OFF", ex.getConflicts().get(0).conflictType());

        verify(appointmentSeriesRepository, never()).save(any());
        verify(appointmentRepository, never()).save(any());
    }
}

package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.InvalidDoctorRoleException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.RegisterDoctorTimeOffCommand;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class RegisterDoctorTimeOffServiceTest {

    private DoctorTimeOffRepository doctorTimeOffRepository;
    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private CurrentUserPort currentUserPort;
    private AuditLogRepository auditLogRepository;
    private ClockPort clockPort;
    private ObjectMapper objectMapper;
    private RegisterDoctorTimeOffService service;

    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-08T08:00:00Z");
    private static final Instant START = Instant.parse("2026-09-10T08:00:00Z");
    private static final Instant END = Instant.parse("2026-09-10T12:00:00Z");

    @BeforeEach
    void setUp() {
        doctorTimeOffRepository = mock(DoctorTimeOffRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        patientRepository = mock(PatientRepository.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        currentUserPort = mock(CurrentUserPort.class);
        auditLogRepository = mock(AuditLogRepository.class);
        clockPort = mock(ClockPort.class);
        objectMapper = new ObjectMapper();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);

        service = new RegisterDoctorTimeOffService(
                doctorTimeOffRepository,
                appointmentRepository,
                patientRepository,
                userRepository,
                roleRepository,
                currentUserPort,
                auditLogRepository,
                clockPort,
                objectMapper
        );
    }

    private User createDoctor(boolean active) {
        return User.restore(
                DOCTOR_ID, "doctor1", "hash", "Dr. Nguyen", "dr@example.com",
                "0901234567", ROLE_ID, active, null, NOW
        );
    }

    private Role createRole(String name) {
        return Role.restore(ROLE_ID, name, name, true, NOW, null, Set.of());
    }

    @Test
    void registerTimeOff_SuccessWithAffectedAppointments_TC03() {
        User doctor = createDoctor(true);
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createRole("DOCTOR")));

        when(doctorTimeOffRepository.save(any(DoctorTimeOff.class))).thenAnswer(inv -> inv.getArgument(0));

        // Stub an existing appointment in the time window
        Appointment appt = Appointment.restore(
                UUID.randomUUID(), "AP123456", PATIENT_ID, DOCTOR_ID,
                START.plusSeconds(1800), START.plusSeconds(3600),
                AppointmentStatus.SCHEDULED, "Khám định kỳ",
                null, null, null, ACTOR_ID, NOW
        );
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(DOCTOR_ID, START, END))
                .thenReturn(List.of(appt));

        Patient patient = mock(Patient.class);
        when(patient.getFullName()).thenReturn("Nguyen Van A");
        when(patient.getPhone()).thenReturn("0912345678");
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

        RegisterDoctorTimeOffCommand command = new RegisterDoctorTimeOffCommand(
                DOCTOR_ID, START, END, "Nghỉ phép cá nhân"
        );

        DoctorTimeOffResult result = service.registerTimeOff(command);

        assertNotNull(result);
        assertEquals(DOCTOR_ID, result.doctorId());
        assertEquals(START, result.startTime());
        assertEquals(END, result.endTime());
        assertEquals("Nghỉ phép cá nhân", result.reason());
        assertEquals(TimeOffStatus.ACTIVE, result.status());

        // TC-03: affected appointments returned
        assertEquals(1, result.affectedAppointments().size());
        assertEquals(appt.getId(), result.affectedAppointments().get(0).id());
        assertEquals("AP123456", result.affectedAppointments().get(0).appointmentCode());
        assertEquals("Nguyen Van A", result.affectedAppointments().get(0).patientFullName());
        assertEquals("0912345678", result.affectedAppointments().get(0).patientPhone());

        // Audit log verified
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();
        assertEquals(ActionType.CREATE, savedAudit.getActionType());
        assertEquals(ResourceType.DOCTOR_TIME_OFF, savedAudit.getResourceType());
    }

    @Test
    void registerTimeOff_ThrowsWhenEndTimeNotAfterStartTime() {
        assertThrows(ValidationException.class,
                () -> service.registerTimeOff(new RegisterDoctorTimeOffCommand(DOCTOR_ID, END, START, "Nghỉ phép")));
    }

    @Test
    void registerTimeOff_ThrowsWhenDoctorNotFound() {
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThrows(DoctorNotFoundException.class,
                () -> service.registerTimeOff(new RegisterDoctorTimeOffCommand(DOCTOR_ID, START, END, "Nghỉ phép")));
    }

    @Test
    void registerTimeOff_ThrowsWhenDoctorInactive() {
        User doctor = createDoctor(false);
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

        assertThrows(DoctorInactiveException.class,
                () -> service.registerTimeOff(new RegisterDoctorTimeOffCommand(DOCTOR_ID, START, END, "Nghỉ phép")));
    }

    @Test
    void registerTimeOff_ThrowsWhenInvalidDoctorRole() {
        UUID otherRoleId = UUID.randomUUID();
        User nonDoctor = User.restore(
                DOCTOR_ID, "doctor1", "hash", "Dr. Nguyen", "dr@example.com",
                "0901234567", otherRoleId, true, null, NOW
        );
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(nonDoctor));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createRole("DOCTOR")));

        assertThrows(InvalidDoctorRoleException.class,
                () -> service.registerTimeOff(new RegisterDoctorTimeOffCommand(DOCTOR_ID, START, END, "Nghỉ phép")));
    }
}

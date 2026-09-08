package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.InvalidDoctorRoleException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.DoctorWeeklyScheduleItemCommand;
import com.benhsoan.port.dto.command.appointment.SetDoctorWeeklyScheduleCommand;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class SetDoctorWeeklyScheduleServiceTest {

    private DoctorWeeklyScheduleRepository doctorWeeklyScheduleRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private ClinicConfigurationRepository clinicConfigurationRepository;
    private CurrentUserPort currentUserPort;
    private AuditLogRepository auditLogRepository;
    private ClockPort clockPort;
    private ObjectMapper objectMapper;
    private SetDoctorWeeklyScheduleService service;

    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-08T08:00:00Z");

    @BeforeEach
    void setUp() {
        doctorWeeklyScheduleRepository = mock(DoctorWeeklyScheduleRepository.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        clinicConfigurationRepository = mock(ClinicConfigurationRepository.class);
        currentUserPort = mock(CurrentUserPort.class);
        auditLogRepository = mock(AuditLogRepository.class);
        clockPort = mock(ClockPort.class);
        objectMapper = new ObjectMapper();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);

        service = new SetDoctorWeeklyScheduleService(
                doctorWeeklyScheduleRepository,
                userRepository,
                roleRepository,
                clinicConfigurationRepository,
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
    void setWeeklySchedule_Success() {
        User doctor = createDoctor(true);
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createRole("DOCTOR")));

        ClinicConfiguration clinic = ClinicConfiguration.create(
                "Clinic", "Address", "0900000000",
                LocalTime.of(7, 30), LocalTime.of(18, 0), NOW
        );
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));

        when(doctorWeeklyScheduleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DoctorWeeklyScheduleItemCommand> items = List.of(
                new DoctorWeeklyScheduleItemCommand(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), true),
                new DoctorWeeklyScheduleItemCommand(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0), true),
                new DoctorWeeklyScheduleItemCommand(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), true)
        );

        DoctorWeeklyScheduleResult result = service.setWeeklySchedule(new SetDoctorWeeklyScheduleCommand(DOCTOR_ID, items));

        assertNotNull(result);
        assertEquals(DOCTOR_ID, result.doctorId());
        assertEquals(3, result.items().size());

        verify(doctorWeeklyScheduleRepository).deleteByDoctorId(DOCTOR_ID);
        verify(doctorWeeklyScheduleRepository).saveAll(any());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();
        assertEquals(ActionType.UPDATE, savedAudit.getActionType());
        assertEquals(ResourceType.DOCTOR_SCHEDULE, savedAudit.getResourceType());
        assertEquals(DOCTOR_ID, savedAudit.getResourceId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void setWeeklySchedule_PreservesActiveFlagFalse() {
        User doctor = createDoctor(true);
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createRole("DOCTOR")));

        ClinicConfiguration clinic = ClinicConfiguration.create(
                "Clinic", "Address", "0900000000",
                LocalTime.of(7, 30), LocalTime.of(18, 0), NOW
        );
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));

        when(doctorWeeklyScheduleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DoctorWeeklyScheduleItemCommand> items = List.of(
                new DoctorWeeklyScheduleItemCommand(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), false)
        );

        DoctorWeeklyScheduleResult result = service.setWeeklySchedule(new SetDoctorWeeklyScheduleCommand(DOCTOR_ID, items));

        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertFalse(result.items().get(0).active());

        ArgumentCaptor<List<DoctorWeeklySchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(doctorWeeklyScheduleRepository).saveAll(captor.capture());
        List<DoctorWeeklySchedule> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertFalse(saved.get(0).isActive());
    }

    @Test
    void setWeeklySchedule_ThrowsWhenDoctorNotFound() {
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThrows(DoctorNotFoundException.class,
                () -> service.setWeeklySchedule(new SetDoctorWeeklyScheduleCommand(DOCTOR_ID, List.of())));
    }

    @Test
    void setWeeklySchedule_ThrowsWhenDoctorInactive() {
        User doctor = createDoctor(false);
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(doctor));

        assertThrows(DoctorInactiveException.class,
                () -> service.setWeeklySchedule(new SetDoctorWeeklyScheduleCommand(DOCTOR_ID, List.of())));
    }

    @Test
    void setWeeklySchedule_ThrowsWhenNotDoctorRole() {
        UUID otherRoleId = UUID.randomUUID();
        User nonDoctor = User.restore(
                DOCTOR_ID, "doctor1", "hash", "Dr. Nguyen", "dr@example.com",
                "0901234567", otherRoleId, true, null, NOW
        );
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(nonDoctor));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createRole("DOCTOR")));

        assertThrows(InvalidDoctorRoleException.class,
                () -> service.setWeeklySchedule(new SetDoctorWeeklyScheduleCommand(DOCTOR_ID, List.of())));
    }

    @Test
    void setWeeklySchedule_ThrowsWhenOutsideClinicHours() {
        User doctor = createDoctor(true);
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createRole("DOCTOR")));

        ClinicConfiguration clinic = ClinicConfiguration.create(
                "Clinic", "Address", "0900000000",
                LocalTime.of(8, 0), LocalTime.of(17, 0), NOW
        );
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));

        List<DoctorWeeklyScheduleItemCommand> items = List.of(
                new DoctorWeeklyScheduleItemCommand(DayOfWeek.MONDAY, LocalTime.of(7, 30), LocalTime.of(12, 0), true)
        );

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.setWeeklySchedule(new SetDoctorWeeklyScheduleCommand(DOCTOR_ID, items)));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("exceeds clinic operating hours"));
    }

    @Test
    void setWeeklySchedule_ThrowsWhenOverlappingOnSameDay() {
        User doctor = createDoctor(true);
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createRole("DOCTOR")));

        List<DoctorWeeklyScheduleItemCommand> items = List.of(
                new DoctorWeeklyScheduleItemCommand(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), true),
                new DoctorWeeklyScheduleItemCommand(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(15, 0), true)
        );

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.setWeeklySchedule(new SetDoctorWeeklyScheduleCommand(DOCTOR_ID, items)));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("Overlapping schedule intervals"));
    }
}

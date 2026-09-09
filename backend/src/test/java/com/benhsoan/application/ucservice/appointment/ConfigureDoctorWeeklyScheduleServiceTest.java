package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.benhsoan.port.dto.command.appointment.ConfigureDoctorWeeklyScheduleCommand;
import com.benhsoan.port.dto.command.appointment.WeeklyScheduleItem;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ConfigureDoctorWeeklyScheduleServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ROLE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock private DoctorWeeklyScheduleRepository weeklyScheduleRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ConfigureDoctorWeeklyScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ConfigureDoctorWeeklyScheduleService(
                weeklyScheduleRepository,
                userRepository,
                roleRepository,
                clinicConfigurationRepository,
                currentUserPort,
                auditLogRepository,
                clockPort,
                objectMapper
        );
    }

    private User createDoctorUser(boolean active, UUID roleId) {
        return User.restore(DOCTOR_ID, "dr_test", "hash", "Dr. Test", "dr@test.com", "0901234567",
                roleId, active, null, NOW);
    }

    private Role createDoctorRole() {
        return Role.restore(DOCTOR_ROLE_ID, "DOCTOR", null, true, NOW, NOW, Set.of());
    }

    private ClinicConfiguration createClinicConfig() {
        return ClinicConfiguration.create(
                "Benh So An Clinic",
                "123 Nguyen Trai",
                "0900000000",
                LocalTime.of(7, 30),
                LocalTime.of(17, 30),
                10,
                NOW
        );
    }

    @Test
    void configuresWeeklyScheduleSuccessfullyAndAudits() {
        // TC-01: Cấu hình lịch làm việc định kỳ theo tuần
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(createDoctorUser(true, DOCTOR_ROLE_ID)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createDoctorRole()));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(createClinicConfig()));
        when(weeklyScheduleRepository.findByDoctorId(DOCTOR_ID)).thenReturn(List.of());
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(weeklyScheduleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfigureDoctorWeeklyScheduleCommand command = new ConfigureDoctorWeeklyScheduleCommand(
                DOCTOR_ID,
                List.of(
                        new WeeklyScheduleItem(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), true),
                        new WeeklyScheduleItem(DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(17, 0), true)
                )
        );

        List<DoctorWeeklyScheduleResult> results = service.configureWeeklySchedule(command);

        assertEquals(2, results.size());
        assertEquals(DayOfWeek.MONDAY, results.get(0).dayOfWeek());
        assertEquals(LocalTime.of(8, 0), results.get(0).startTime());
        assertEquals(LocalTime.of(12, 0), results.get(0).endTime());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ActionType.UPDATE, audit.getActionType());
        assertEquals(ResourceType.DOCTOR_SCHEDULE, audit.getResourceType());
        assertEquals(DOCTOR_ID, audit.getResourceId());
    }

    @Test
    void rejectsWhenWorkingHoursFallOutsideClinicHours() {
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(createDoctorUser(true, DOCTOR_ROLE_ID)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createDoctorRole()));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(createClinicConfig())); // 07:30 - 17:30

        // Schedule ends at 18:00 > 17:30
        ConfigureDoctorWeeklyScheduleCommand command = new ConfigureDoctorWeeklyScheduleCommand(
                DOCTOR_ID,
                List.of(new WeeklyScheduleItem(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true))
        );

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.configureWeeklySchedule(command));
        assertNotNull(ex.getMessage());
    }

    @Test
    void rejectsDuplicateDayOfWeekInRequest() {
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(createDoctorUser(true, DOCTOR_ROLE_ID)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createDoctorRole()));

        ConfigureDoctorWeeklyScheduleCommand command = new ConfigureDoctorWeeklyScheduleCommand(
                DOCTOR_ID,
                List.of(
                        new WeeklyScheduleItem(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), true),
                        new WeeklyScheduleItem(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0), true)
                )
        );

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.configureWeeklySchedule(command));
        assertEquals("Ngày trong tuần không được trùng lặp: MONDAY", ex.getMessage());
    }

    @Test
    void rejectsDoctorNotFound() {
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.empty());

        ConfigureDoctorWeeklyScheduleCommand command = new ConfigureDoctorWeeklyScheduleCommand(
                DOCTOR_ID,
                List.of(new WeeklyScheduleItem(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), true))
        );

        assertThrows(DoctorNotFoundException.class, () -> service.configureWeeklySchedule(command));
    }

    @Test
    void rejectsInactiveDoctor() {
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(createDoctorUser(false, DOCTOR_ROLE_ID)));

        ConfigureDoctorWeeklyScheduleCommand command = new ConfigureDoctorWeeklyScheduleCommand(
                DOCTOR_ID,
                List.of(new WeeklyScheduleItem(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), true))
        );

        assertThrows(DoctorInactiveException.class, () -> service.configureWeeklySchedule(command));
    }

    @Test
    void rejectsNonDoctorUser() {
        UUID otherRoleId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(createDoctorUser(true, otherRoleId)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createDoctorRole()));

        ConfigureDoctorWeeklyScheduleCommand command = new ConfigureDoctorWeeklyScheduleCommand(
                DOCTOR_ID,
                List.of(new WeeklyScheduleItem(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), true))
        );

        assertThrows(InvalidDoctorRoleException.class, () -> service.configureWeeklySchedule(command));
    }

    @Test
    void deactivatesOmittedDaysWhenConfiguringWeeklySchedule() {
        // Finding [P2-2]: Semantics của PUT /weekly: các ngày có trong DB nhưng không có trong payload
        // phải tự động được cập nhật active = false (Collection Replacement)
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(createDoctorUser(true, DOCTOR_ROLE_ID)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createDoctorRole()));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(createClinicConfig()));
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);

        DoctorWeeklySchedule existingMonday = DoctorWeeklySchedule.create(
                DOCTOR_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), NOW);
        DoctorWeeklySchedule existingSunday = DoctorWeeklySchedule.create(
                DOCTOR_ID, DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), NOW);

        when(weeklyScheduleRepository.findByDoctorId(DOCTOR_ID))
                .thenReturn(List.of(existingMonday, existingSunday));
        when(weeklyScheduleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // Chỉ gửi Thứ 2, không gửi Chủ Nhật
        ConfigureDoctorWeeklyScheduleCommand command = new ConfigureDoctorWeeklyScheduleCommand(
                DOCTOR_ID,
                List.of(
                        new WeeklyScheduleItem(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), true)
                )
        );

        List<DoctorWeeklyScheduleResult> results = service.configureWeeklySchedule(command);

        // Phải trả về cả Thứ 2 (active = true) và Chủ Nhật (active = false)
        assertEquals(2, results.size());

        DoctorWeeklyScheduleResult mondayResult = results.stream()
                .filter(r -> r.dayOfWeek() == DayOfWeek.MONDAY)
                .findFirst().orElseThrow();
        assertEquals(true, mondayResult.active());
        assertEquals(LocalTime.of(9, 0), mondayResult.startTime());

        DoctorWeeklyScheduleResult sundayResult = results.stream()
                .filter(r -> r.dayOfWeek() == DayOfWeek.SUNDAY)
                .findFirst().orElseThrow();
        assertEquals(false, sundayResult.active());
    }
}

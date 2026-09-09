package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.DoctorTimeOffConflictException;
import com.benhsoan.domain.appointment.exception.InvalidDoctorRoleException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.RegisterDoctorTimeOffCommand;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RegisterDoctorTimeOffServiceTest {

        private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");
        private static final UUID DOCTOR_ID = UUID.randomUUID();
        private static final UUID DOCTOR_ROLE_ID = UUID.randomUUID();
        private static final UUID ACTOR_ID = UUID.randomUUID();

        @Mock
        private DoctorTimeOffRepository doctorTimeOffRepository;
        @Mock
        private AppointmentRepository appointmentRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private RoleRepository roleRepository;
        @Mock
        private CurrentUserPort currentUserPort;
        @Mock
        private AuditLogRepository auditLogRepository;
        @Mock
        private ClockPort clockPort;

        private final ObjectMapper objectMapper = new ObjectMapper();
        private RegisterDoctorTimeOffService service;

        @BeforeEach
        void setUp() {
                service = new RegisterDoctorTimeOffService(
                                doctorTimeOffRepository,
                                appointmentRepository,
                                userRepository,
                                roleRepository,
                                currentUserPort,
                                auditLogRepository,
                                clockPort,
                                objectMapper);
        }

        private User createDoctorUser(boolean active, UUID roleId) {
                return User.restore(DOCTOR_ID, "dr_test", "hash", "Dr. Test", "dr@test.com", "0901234567",
                                roleId, active, null, NOW);
        }

        private Role createDoctorRole() {
                return Role.restore(DOCTOR_ROLE_ID, "DOCTOR", null, true, NOW, NOW, Set.of());
        }

        @Test
        void registersTimeOffSuccessfullyWithoutAffectedAppointments() {
                Instant startTime = NOW.plusSeconds(3600);
                Instant endTime = startTime.plusSeconds(7200);

                when(clockPort.now()).thenReturn(NOW);
                when(userRepository.findByIdForUpdate(DOCTOR_ID))
                                .thenReturn(Optional.of(createDoctorUser(true, DOCTOR_ROLE_ID)));
                when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createDoctorRole()));
                when(doctorTimeOffRepository.existsActiveOverlapping(DOCTOR_ID, startTime, endTime)).thenReturn(false);
                when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
                when(doctorTimeOffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(appointmentRepository.findActiveAppointmentsForDoctorBetween(DOCTOR_ID, startTime, endTime))
                                .thenReturn(List.of());

                RegisterDoctorTimeOffCommand command = new RegisterDoctorTimeOffCommand(
                                DOCTOR_ID, startTime, endTime, "Đi hội nghị chuyên ngành");

                DoctorTimeOffResult result = service.registerTimeOff(command);

                assertNotNull(result);
                assertEquals(DOCTOR_ID, result.doctorId());
                assertEquals(startTime, result.startTime());
                assertEquals(endTime, result.endTime());
                assertEquals(TimeOffStatus.ACTIVE, result.status());
                assertTrue(result.affectedAppointments().isEmpty());

                ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(auditCaptor.capture());
                AuditLog audit = auditCaptor.getValue();
                assertEquals(ActionType.CREATE, audit.getActionType());
                assertEquals(ResourceType.DOCTOR_TIMEOFF, audit.getResourceType());
        }

        @Test
        void registersTimeOffWithAffectedAppointmentsTC03() {
                // TC-03: Đăng ký khoảng nghỉ khi có lịch hẹn trùng lặp
                // Hệ thống lưu khoảng nghỉ thành công và trả về danh sách lịch hẹn bị ảnh hưởng
                Instant startTime = NOW.plusSeconds(3600);
                Instant endTime = startTime.plusSeconds(7200);

                when(clockPort.now()).thenReturn(NOW);
                when(userRepository.findByIdForUpdate(DOCTOR_ID))
                                .thenReturn(Optional.of(createDoctorUser(true, DOCTOR_ROLE_ID)));
                when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createDoctorRole()));
                when(doctorTimeOffRepository.existsActiveOverlapping(DOCTOR_ID, startTime, endTime)).thenReturn(false);
                when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
                when(doctorTimeOffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                UUID patientId = UUID.randomUUID();
                Appointment app1 = Appointment.restore(
                                UUID.randomUUID(), "AP-0001", patientId, DOCTOR_ID,
                                startTime, startTime.plusSeconds(1800), AppointmentStatus.SCHEDULED,
                                "Khám định kỳ", null, null, null, ACTOR_ID, NOW);
                Appointment app2 = Appointment.restore(
                                UUID.randomUUID(), "AP-0002", patientId, DOCTOR_ID,
                                startTime.plusSeconds(1800), startTime.plusSeconds(3600), AppointmentStatus.CONFIRMED,
                                "Tái khám", null, null, null, ACTOR_ID, NOW);

                when(appointmentRepository.findActiveAppointmentsForDoctorBetween(DOCTOR_ID, startTime, endTime))
                                .thenReturn(List.of(app1, app2));

                RegisterDoctorTimeOffCommand command = new RegisterDoctorTimeOffCommand(
                                DOCTOR_ID, startTime, endTime, "Nghỉ phép đột xuất");

                DoctorTimeOffResult result = service.registerTimeOff(command);

                assertNotNull(result);
                assertEquals(TimeOffStatus.ACTIVE, result.status());
                assertFalse(result.affectedAppointments().isEmpty());
                assertEquals(2, result.affectedAppointments().size());
                assertEquals("AP-0001", result.affectedAppointments().get(0).appointmentCode());
                assertEquals("AP-0002", result.affectedAppointments().get(1).appointmentCode());
        }

        @Test
        void rejectsOverlappingTimeOff() {
                Instant startTime = NOW.plusSeconds(3600);
                Instant endTime = startTime.plusSeconds(7200);

                when(clockPort.now()).thenReturn(NOW);
                when(userRepository.findByIdForUpdate(DOCTOR_ID))
                                .thenReturn(Optional.of(createDoctorUser(true, DOCTOR_ROLE_ID)));
                when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createDoctorRole()));
                when(doctorTimeOffRepository.existsActiveOverlapping(DOCTOR_ID, startTime, endTime)).thenReturn(true);

                RegisterDoctorTimeOffCommand command = new RegisterDoctorTimeOffCommand(
                                DOCTOR_ID, startTime, endTime, "Nghỉ phép");

                assertThrows(DoctorTimeOffConflictException.class, () -> service.registerTimeOff(command));
        }

        @Test
        void rejectsPastStartTime() {
                Instant startTime = NOW.minusSeconds(3600);
                Instant endTime = NOW.plusSeconds(3600);

                when(clockPort.now()).thenReturn(NOW);

                RegisterDoctorTimeOffCommand command = new RegisterDoctorTimeOffCommand(
                                DOCTOR_ID, startTime, endTime, "Nghỉ phép");

                ValidationException ex = assertThrows(ValidationException.class,
                                () -> service.registerTimeOff(command));
                assertEquals("Thời gian nghỉ không được bắt đầu trong quá khứ.", ex.getMessage());
        }

        @Test
        void rejectsEndTimeBeforeStartTime() {
                Instant startTime = NOW.plusSeconds(7200);
                Instant endTime = NOW.plusSeconds(3600);

                when(clockPort.now()).thenReturn(NOW);

                RegisterDoctorTimeOffCommand command = new RegisterDoctorTimeOffCommand(
                                DOCTOR_ID, startTime, endTime, "Nghỉ phép");

                ValidationException ex = assertThrows(ValidationException.class,
                                () -> service.registerTimeOff(command));
                assertEquals("Thời gian kết thúc nghỉ phải sau thời gian bắt đầu.", ex.getMessage());
        }

        @Test
        void rejectsDoctorNotFound() {
                Instant startTime = NOW.plusSeconds(3600);
                Instant endTime = startTime.plusSeconds(7200);

                when(clockPort.now()).thenReturn(NOW);
                when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.empty());

                RegisterDoctorTimeOffCommand command = new RegisterDoctorTimeOffCommand(
                                DOCTOR_ID, startTime, endTime, "Nghỉ phép");

                assertThrows(DoctorNotFoundException.class, () -> service.registerTimeOff(command));
        }

        @Test
        void rejectsInactiveDoctor() {
                Instant startTime = NOW.plusSeconds(3600);
                Instant endTime = startTime.plusSeconds(7200);

                when(clockPort.now()).thenReturn(NOW);
                when(userRepository.findByIdForUpdate(DOCTOR_ID))
                                .thenReturn(Optional.of(createDoctorUser(false, DOCTOR_ROLE_ID)));

                RegisterDoctorTimeOffCommand command = new RegisterDoctorTimeOffCommand(
                                DOCTOR_ID, startTime, endTime, "Nghỉ phép");

                assertThrows(DoctorInactiveException.class, () -> service.registerTimeOff(command));
        }

        @Test
        void rejectsNonDoctorUser() {
                Instant startTime = NOW.plusSeconds(3600);
                Instant endTime = startTime.plusSeconds(7200);
                UUID receptionistRoleId = UUID.randomUUID();

                when(clockPort.now()).thenReturn(NOW);
                when(userRepository.findByIdForUpdate(DOCTOR_ID))
                                .thenReturn(Optional.of(createDoctorUser(true, receptionistRoleId)));
                when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(createDoctorRole()));

                RegisterDoctorTimeOffCommand command = new RegisterDoctorTimeOffCommand(
                                DOCTOR_ID, startTime, endTime, "Nghỉ phép");

                assertThrows(InvalidDoctorRoleException.class, () -> service.registerTimeOff(command));
        }
}

package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.benhsoan.application.ucservice.portal.PatientPortalNotificationCreator;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentRescheduleLog;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentInvalidStatusException;
import com.benhsoan.domain.appointment.exception.AppointmentPastCutoffException;
import com.benhsoan.domain.appointment.exception.AppointmentTimeConflictException;
import com.benhsoan.domain.appointment.exception.AppointmentTimeInPastException;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotWorkingException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.RescheduleAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRescheduleLogRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RescheduleAppointmentServiceTest {

        @Mock
        private AppointmentRepository appointmentRepository;
        @Mock
        private AppointmentRescheduleLogRepository rescheduleLogRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private DoctorScheduleValidator doctorScheduleValidator;
        @Mock
        private CurrentUserPort currentUserPort;
        @Mock
        private AuditLogRepository auditLogRepository;
        @Mock
        private ClockPort clockPort;
        @Mock
        private AppointmentAccessDeniedAuditWriter accessDeniedAuditWriter;
        @Mock
        private PatientPortalNotificationCreator patientPortalNotificationCreator;

        private AppointmentResultMapper resultMapper;
        private AppointmentRescheduleHistoryAssembler historyAssembler;
        private RescheduleAppointmentService service;
        private final ObjectMapper objectMapper = new ObjectMapper();

        private final Instant fixedNow = Instant.parse("2026-09-14T08:00:00Z");
        private final UUID receptionistId = UUID.randomUUID();
        private final UUID doctor1Id = UUID.randomUUID();
        private final UUID doctor2Id = UUID.randomUUID();
        private final UUID patientId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                resultMapper = new AppointmentResultMapper();
                historyAssembler = new AppointmentRescheduleHistoryAssembler(rescheduleLogRepository, userRepository);
                service = new RescheduleAppointmentService(
                                appointmentRepository,
                                rescheduleLogRepository,
                                userRepository,
                                doctorScheduleValidator,
                                currentUserPort,
                                auditLogRepository,
                                clockPort,
                                resultMapper,
                                historyAssembler,
                                accessDeniedAuditWriter,
                                patientPortalNotificationCreator,
                                objectMapper);
        }

        private User createDoctorUser(UUID doctorId, String fullName, boolean active) {
                return User.restore(
                                doctorId, "doctor_" + doctorId, "hash", fullName, "doc@test.com", "0900000000",
                                UUID.randomUUID(), active, null, fixedNow);
        }

        private Appointment createTestAppointment(UUID doctorId, AppointmentStatus status, Instant startTime,
                        Instant endTime) {
                return Appointment.restore(
                                UUID.randomUUID(), "APT000100", patientId, doctorId, startTime, endTime,
                                status, "Kham tong quat", null, null, null, receptionistId,
                                fixedNow.minusSeconds(3600));
        }

        @Test
        void reschedulesSuccessfullyForSameDoctor_TC01() {
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = createTestAppointment(doctor1Id, AppointmentStatus.SCHEDULED,
                                fixedNow.plusSeconds(3600), fixedNow.plusSeconds(5400));
                User doctor = createDoctorUser(doctor1Id, "Dr. Nguyen", true);

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);
                when(clockPort.now()).thenReturn(fixedNow);
                when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
                when(userRepository.findByIdForUpdate(doctor1Id)).thenReturn(Optional.of(doctor));
                when(appointmentRepository.findActiveAppointmentsForDoctorBetween(any(), any(), any()))
                                .thenReturn(List.of());
                when(appointmentRepository.save(any(Appointment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Instant newStart = fixedNow.plusSeconds(7200);
                Instant newEnd = fixedNow.plusSeconds(9000);
                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .startTime(newStart)
                                .endTime(newEnd)
                                .reason("Bệnh nhân xin dời giờ do kẹt xe")
                                .build();

                AppointmentResult result = service.reschedule(appointmentId, command);

                assertNotNull(result);
                assertEquals(newStart, result.startTime());
                assertEquals(newEnd, result.endTime());
                assertEquals("Kham tong quat", result.reason());

                verify(rescheduleLogRepository).save(any(AppointmentRescheduleLog.class));
                verify(auditLogRepository).save(any(AuditLog.class));
                verify(patientPortalNotificationCreator).createAppointmentChanged(
                                any(Appointment.class), any(AppointmentRescheduleLog.class), eq(fixedNow));
        }

        @Test
        void reschedulesSuccessfullyForDifferentDoctor_TC01b() {
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = createTestAppointment(doctor1Id, AppointmentStatus.SCHEDULED,
                                fixedNow.plusSeconds(3600), fixedNow.plusSeconds(5400));
                User doctor2 = createDoctorUser(doctor2Id, "Dr. Tran", true);

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);
                when(clockPort.now()).thenReturn(fixedNow);
                when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
                when(userRepository.findByIdForUpdate(doctor2Id)).thenReturn(Optional.of(doctor2));
                when(appointmentRepository.findActiveAppointmentsForDoctorBetween(any(), any(), any()))
                                .thenReturn(List.of());
                when(appointmentRepository.save(any(Appointment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Instant newStart = fixedNow.plusSeconds(7200);
                Instant newEnd = fixedNow.plusSeconds(9000);
                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .newDoctorId(doctor2Id)
                                .startTime(newStart)
                                .endTime(newEnd)
                                .reason("Bác sĩ 1 có việc đột xuất")
                                .build();

                AppointmentResult result = service.reschedule(appointmentId, command);

                assertNotNull(result);
                assertEquals(doctor2Id, result.doctorId());
                assertEquals(newStart, result.startTime());
                assertEquals("Kham tong quat", result.reason());
                verify(rescheduleLogRepository).save(any(AppointmentRescheduleLog.class));
        }

        @Test
        void rejectsWhenUserLacksPermission() {
                UUID appointmentId = UUID.randomUUID();
                when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
                when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);
                when(clockPort.now()).thenReturn(fixedNow);

                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .startTime(fixedNow.plusSeconds(3600))
                                .endTime(fixedNow.plusSeconds(5400))
                                .reason("Dời lịch")
                                .build();

                assertThrows(UnauthorizedAppointmentOperationException.class,
                                () -> service.reschedule(appointmentId, command));
                verify(accessDeniedAuditWriter).writeRescheduleDenied(eq(receptionistId), eq(appointmentId),
                                eq(fixedNow), any());
        }

        @Test
        void rejectsWhenCurrentAppointmentIsPastCutoff_TC02() {
                UUID appointmentId = UUID.randomUUID();
                // Start time in the past
                Appointment appointment = createTestAppointment(doctor1Id, AppointmentStatus.SCHEDULED,
                                fixedNow.minusSeconds(1800), fixedNow.minusSeconds(600));

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(clockPort.now()).thenReturn(fixedNow);
                when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
                when(userRepository.findByIdForUpdate(doctor1Id))
                                .thenReturn(Optional.of(createDoctorUser(doctor1Id, "Dr. Nguyen", true)));

                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .startTime(fixedNow.plusSeconds(3600))
                                .endTime(fixedNow.plusSeconds(5400))
                                .reason("Dời lịch trễ")
                                .build();

                assertThrows(AppointmentPastCutoffException.class,
                                () -> service.reschedule(appointmentId, command));
        }

        @Test
        void rejectsWhenAppointmentStatusIsInvalid_TC02b() {
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = createTestAppointment(doctor1Id, AppointmentStatus.NO_SHOW,
                                fixedNow.plusSeconds(3600), fixedNow.plusSeconds(5400));

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(clockPort.now()).thenReturn(fixedNow);
                when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
                when(userRepository.findByIdForUpdate(doctor1Id))
                                .thenReturn(Optional.of(createDoctorUser(doctor1Id, "Dr. Nguyen", true)));

                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .startTime(fixedNow.plusSeconds(7200))
                                .endTime(fixedNow.plusSeconds(9000))
                                .reason("Dời lịch no-show")
                                .build();

                assertThrows(AppointmentInvalidStatusException.class,
                                () -> service.reschedule(appointmentId, command));
        }

        @Test
        void rejectsWhenTargetDoctorHasTimeConflict_TC03_QTN04() {
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = createTestAppointment(doctor1Id, AppointmentStatus.SCHEDULED,
                                fixedNow.plusSeconds(3600), fixedNow.plusSeconds(5400));
                Appointment conflictingApp = createTestAppointment(doctor1Id, AppointmentStatus.SCHEDULED,
                                fixedNow.plusSeconds(7200), fixedNow.plusSeconds(9000));

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(clockPort.now()).thenReturn(fixedNow);
                when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
                when(userRepository.findByIdForUpdate(doctor1Id))
                                .thenReturn(Optional.of(createDoctorUser(doctor1Id, "Dr. Nguyen", true)));

                Instant newStart = fixedNow.plusSeconds(7200);
                Instant newEnd = fixedNow.plusSeconds(9000);
                when(appointmentRepository.findActiveAppointmentsForDoctorBetween(doctor1Id, newStart, newEnd))
                                .thenReturn(List.of(conflictingApp));

                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .startTime(newStart)
                                .endTime(newEnd)
                                .reason("Dời giờ trùng")
                                .build();

                assertThrows(AppointmentTimeConflictException.class,
                                () -> service.reschedule(appointmentId, command));
        }

        @Test
        void rejectsWhenTargetDoctorNotWorking_QTN30() {
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = createTestAppointment(doctor1Id, AppointmentStatus.SCHEDULED,
                                fixedNow.plusSeconds(3600), fixedNow.plusSeconds(5400));

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(clockPort.now()).thenReturn(fixedNow);
                when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
                when(userRepository.findByIdForUpdate(doctor1Id))
                                .thenReturn(Optional.of(createDoctorUser(doctor1Id, "Dr. Nguyen", true)));

                Instant newStart = fixedNow.plusSeconds(7200);
                Instant newEnd = fixedNow.plusSeconds(9000);
                doThrow(new DoctorNotWorkingException("Bác sĩ không làm việc trong khung giờ này."))
                                .when(doctorScheduleValidator)
                                .validateDoctorWorkingAndAvailable(doctor1Id, newStart, newEnd);

                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .startTime(newStart)
                                .endTime(newEnd)
                                .reason("Dời giờ vào ngày nghỉ")
                                .build();

                assertThrows(DoctorNotWorkingException.class,
                                () -> service.reschedule(appointmentId, command));
        }

        @Test
        void rejectsWhenDoctorInactive() {
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = createTestAppointment(doctor1Id, AppointmentStatus.SCHEDULED,
                                fixedNow.plusSeconds(3600), fixedNow.plusSeconds(5400));
                User inactiveDoctor = createDoctorUser(doctor1Id, "Dr. Inactive", false);

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(clockPort.now()).thenReturn(fixedNow);
                when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
                when(userRepository.findByIdForUpdate(doctor1Id)).thenReturn(Optional.of(inactiveDoctor));

                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .startTime(fixedNow.plusSeconds(7200))
                                .endTime(fixedNow.plusSeconds(9000))
                                .reason("Dời giờ")
                                .build();

                assertThrows(DoctorInactiveException.class,
                                () -> service.reschedule(appointmentId, command));
        }

        @Test
        void rejectsWhenNewStartTimeIsInPast() {
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = createTestAppointment(doctor1Id, AppointmentStatus.SCHEDULED,
                                fixedNow.plusSeconds(3600), fixedNow.plusSeconds(5400));
                when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));

                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .startTime(fixedNow.minusSeconds(300))
                                .endTime(fixedNow.plusSeconds(1800))
                                .reason("Dời giờ quá khứ")
                                .build();

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(clockPort.now()).thenReturn(fixedNow);

                assertThrows(AppointmentTimeInPastException.class,
                                () -> service.reschedule(appointmentId, command));
        }

        @Test
        void rejectsWhenEndTimeBeforeStartTime() {
                RescheduleAppointmentCommand command = RescheduleAppointmentCommand.builder()
                                .startTime(fixedNow.plusSeconds(3600))
                                .endTime(fixedNow.plusSeconds(1800))
                                .reason("Dời giờ sai mốc")
                                .build();

                assertThrows(ValidationException.class,
                                () -> service.reschedule(UUID.randomUUID(), command));
        }
}

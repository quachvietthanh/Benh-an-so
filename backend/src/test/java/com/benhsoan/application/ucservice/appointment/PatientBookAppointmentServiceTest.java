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
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.junit.jupiter.api.DisplayName;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.DoctorNotWorkingException;
import com.benhsoan.domain.appointment.exception.DoctorScheduleNotFoundException;
import com.benhsoan.domain.appointment.exception.DoctorUnavailableException;
import com.benhsoan.domain.appointment.exception.InvalidAppointmentTimeException;
import com.benhsoan.domain.appointment.exception.InvalidDoctorRoleException;
import com.benhsoan.domain.appointment.exception.SlotAlreadyBookedException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.command.appointment.PatientBookAppointmentCommand;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
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
    @Mock private com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository doctorWeeklyScheduleRepository;
    @Mock private com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository doctorTimeOffRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;
    @Mock private com.benhsoan.application.ucservice.patient.PatientAccessGuard patientAccessGuard;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PatientBookAppointmentService service;

    @BeforeEach
    void setUp() {
        service = new PatientBookAppointmentService(
                appointmentRepository,
                appointmentCodeGenerator,
                doctorScheduleRepository,
                doctorWeeklyScheduleRepository,
                doctorTimeOffRepository,
                patientRepository,
                userRepository,
                roleRepository,
                currentUserPort,
                auditLogRepository,
                clockPort,
                objectMapper,
                patientAccessGuard
        );
    }

    private User doctor(UUID doctorId, UUID roleId) {
        return User.restore(doctorId, "doctor1", "hash", "Doctor One", "doctor1@example.com",
                "0900000001", roleId, true, null, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private Role doctorRole(UUID roleId) {
        return Role.restore(roleId, "DOCTOR", null, true, NOW, NOW, Set.of());
    }

    private DoctorSchedule schedule(UUID doctorId, LocalDate date, LocalTime start, LocalTime end, boolean active) {
        return DoctorSchedule.restore(UUID.randomUUID(), doctorId, date, start, end, active, NOW, null);
    }

    private void stubPatientAndDoctor(UUID userId, UUID patientId, UUID doctorId, UUID roleId) {
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        // An own patient profile is linked to the account, so the target is not "on behalf".
        org.mockito.Mockito.lenient().when(patient.getUserId()).thenReturn(userId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor(doctorId, roleId)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole(roleId)));
    }

    @Test
    void booksAppointmentWithOnlinePortalChannelAndWritesAuditLog() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        stubPatientAndDoctor(userId, patientId, doctorId, roleId);
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.of(schedule(doctorId, FUTURE_DATE, LocalTime.of(8, 0), LocalTime.of(17, 0), true)));
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
        // NCL-14-CN-010: own-profile booking records the actor and no on-behalf flag.
        assertEquals(userId.toString(), node.get("bookedByUserId").asText());
        org.junit.jupiter.api.Assertions.assertFalse(
                node.get("bookingOnBehalfOfDependent").asBoolean());
    }

    @Test
    void rejectsSlotCollisionWithConflict() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        stubPatientAndDoctor(userId, patientId, doctorId, roleId);
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.of(schedule(doctorId, FUTURE_DATE, LocalTime.of(8, 0), LocalTime.of(17, 0), true)));

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

    @Test
    void rejectsMissingDoctorSchedule() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        stubPatientAndDoctor(userId, patientId, doctorId, roleId);
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.empty());

        assertThrows(DoctorScheduleNotFoundException.class,
                () -> service.book(new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, START_TIME, null)));
    }

    @Test
    void rejectsInactiveDoctorSchedule() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        stubPatientAndDoctor(userId, patientId, doctorId, roleId);
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.of(schedule(doctorId, FUTURE_DATE, LocalTime.of(8, 0), LocalTime.of(17, 0), false)));

        assertThrows(DoctorUnavailableException.class,
                () -> service.book(new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, START_TIME, null)));
    }

    @Test
    void rejectsUnalignedSlotTime() {
        assertThrows(InvalidAppointmentTimeException.class,
                () -> service.book(new PatientBookAppointmentCommand(
                        UUID.randomUUID(), FUTURE_DATE, LocalTime.of(9, 17), null)));
    }

    @Test
    void rejectsSlotOutsideWorkingHours() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        stubPatientAndDoctor(userId, patientId, doctorId, roleId);
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.of(schedule(doctorId, FUTURE_DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), true)));

        assertThrows(InvalidAppointmentTimeException.class,
                () -> service.book(new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, LocalTime.of(10, 0), null)));
    }

    @Test
    void rejectsNonDoctorRole() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID patientRoleId = UUID.randomUUID();
        UUID doctorRoleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        // An own patient profile is linked to the account, so the target is not "on behalf".
        org.mockito.Mockito.lenient().when(patient.getUserId()).thenReturn(userId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor(doctorId, patientRoleId)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole(doctorRoleId)));

        assertThrows(InvalidDoctorRoleException.class,
                () -> service.book(new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, START_TIME, null)));
    }

    @Test
    void rejectsBookingWhenDoctorInTimeOff() {
        // TC-02 / QTN-30: Bác sĩ không làm việc trong khung giờ này
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID doctorRoleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        // An own patient profile is linked to the account, so the target is not "on behalf".
        org.mockito.Mockito.lenient().when(patient.getUserId()).thenReturn(userId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor(doctorId, doctorRoleId)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole(doctorRoleId)));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.of(schedule(doctorId, FUTURE_DATE, LocalTime.of(8, 0), LocalTime.of(12, 0), true)));
        when(doctorTimeOffRepository.existsActiveOverlapping(eq(doctorId), any(Instant.class), any(Instant.class)))
                .thenReturn(true);

        DoctorNotWorkingException ex = assertThrows(DoctorNotWorkingException.class,
                () -> service.book(new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, START_TIME, null)));
        assertEquals("Bác sĩ không làm việc trong khung giờ này.", ex.getMessage());
    }

    @Test
    void booksSuccessfullyUsingWeeklyScheduleFallback() {
        // TC-01: Đặt lịch dựa trên lịch tuần định kỳ khi không có lịch ngày cụ thể
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID doctorRoleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        // An own patient profile is linked to the account, so the target is not "on behalf".
        org.mockito.Mockito.lenient().when(patient.getUserId()).thenReturn(userId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor(doctorId, doctorRoleId)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole(doctorRoleId)));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.empty());

        DoctorWeeklySchedule weeklySchedule = DoctorWeeklySchedule.create(
                doctorId, FUTURE_DATE.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(12, 0), NOW);
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, FUTURE_DATE.getDayOfWeek()))
                .thenReturn(Optional.of(weeklySchedule));
        when(doctorTimeOffRepository.existsActiveOverlapping(eq(doctorId), any(Instant.class), any(Instant.class)))
                .thenReturn(false);
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(eq(doctorId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(appointmentCodeGenerator.generate()).thenReturn("AP-12345");
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientAppointmentResult result = service.book(
                new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, START_TIME, "Khám bệnh"));

        assertEquals("AP-12345", result.appointmentCode());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void rejectsBookingWhenDayIsDeactivatedInWeeklySchedule() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID doctorRoleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        // An own patient profile is linked to the account, so the target is not "on behalf".
        org.mockito.Mockito.lenient().when(patient.getUserId()).thenReturn(userId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor(doctorId, doctorRoleId)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole(doctorRoleId)));

        DoctorWeeklySchedule weeklySchedule = DoctorWeeklySchedule.create(
                doctorId, FUTURE_DATE.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(12, 0), NOW);
        weeklySchedule.update(LocalTime.of(8, 0), LocalTime.of(12, 0), false, NOW);
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, FUTURE_DATE.getDayOfWeek()))
                .thenReturn(Optional.of(weeklySchedule));

        assertThrows(DoctorUnavailableException.class,
                () -> service.book(new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, START_TIME, "Khám bệnh")));
    }

    @Test
    void autoSchedulesWaitlistEntryWhenPatientBooksOnline() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID doctorRoleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        // An own patient profile is linked to the account, so the target is not "on behalf".
        org.mockito.Mockito.lenient().when(patient.getUserId()).thenReturn(userId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor(doctorId, doctorRoleId)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole(doctorRoleId)));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.empty());

        DoctorWeeklySchedule weeklySchedule = DoctorWeeklySchedule.create(
                doctorId, FUTURE_DATE.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(12, 0), NOW);
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, FUTURE_DATE.getDayOfWeek()))
                .thenReturn(Optional.of(weeklySchedule));
        when(doctorTimeOffRepository.existsActiveOverlapping(eq(doctorId), any(Instant.class), any(Instant.class)))
                .thenReturn(false);
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(eq(doctorId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(appointmentCodeGenerator.generate()).thenReturn("AP-WAITLIST");
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository waitlistRepo =
                mock(com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository.class);
        com.benhsoan.domain.appointment.AppointmentWaitlist waitlistEntry =
                com.benhsoan.domain.appointment.AppointmentWaitlist.create(
                        patientId, doctorId, FUTURE_DATE,
                        com.benhsoan.domain.appointment.enums.TimePreference.ANYTIME,
                        "Cần khám sớm", userId, NOW);

        when(waitlistRepo.findActiveByPatientAndDoctorAndDate(patientId, doctorId, FUTURE_DATE))
                .thenReturn(Optional.of(waitlistEntry));

        PatientBookAppointmentService serviceWithWaitlist = new PatientBookAppointmentService(
                appointmentRepository,
                appointmentCodeGenerator,
                doctorScheduleRepository,
                doctorWeeklyScheduleRepository,
                doctorTimeOffRepository,
                patientRepository,
                userRepository,
                roleRepository,
                currentUserPort,
                auditLogRepository,
                clockPort,
                objectMapper,
                waitlistRepo,
                patientAccessGuard
        );

        PatientAppointmentResult result = serviceWithWaitlist.book(
                new PatientBookAppointmentCommand(doctorId, FUTURE_DATE, START_TIME, "Khám bệnh"));

        assertEquals("AP-WAITLIST", result.appointmentCode());
        assertEquals(com.benhsoan.domain.appointment.enums.WaitlistStatus.SCHEDULED, waitlistEntry.getStatus());
        verify(waitlistRepo).save(waitlistEntry);
    }

    // ------------------------------------------------------------------
    // NCL-14-CN-010: booking on behalf of a linked dependent patient
    // ------------------------------------------------------------------

    @Test
    @DisplayName("P2.2: khong dat lich cho ho so phu thuoc da bi gop (MERGED)")
    void rejectsBookingForMergedDependent() {
        UUID userId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();
        UUID mergedIntoId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);

        Patient merged = mock(Patient.class);
        when(patientAccessGuard.requirePatientAccess(dependentId)).thenReturn(merged);
        // Domain lifecycle guard rejects merged profiles for NEW activity.
        org.mockito.Mockito.doThrow(new com.benhsoan.domain.patient.exception
                        .PatientAlreadyMergedException(dependentId, mergedIntoId))
                .when(merged).validateCanReceiveNewActivity();

        assertThrows(com.benhsoan.domain.patient.exception.PatientAlreadyMergedException.class,
                () -> service.book(new PatientBookAppointmentCommand(
                        UUID.randomUUID(), FUTURE_DATE, START_TIME, "Kham nhi", dependentId)));

        // Nothing is persisted or audited for a rejected target.
        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("P2.2: khong dat lich cho ho so phu thuoc da bi vo hieu hoa (INACTIVE)")
    void rejectsBookingForInactiveDependent() {
        UUID userId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);

        Patient inactive = mock(Patient.class);
        when(patientAccessGuard.requirePatientAccess(dependentId)).thenReturn(inactive);
        org.mockito.Mockito.doThrow(new com.benhsoan.domain.patient.exception
                        .PatientInactiveException())
                .when(inactive).validateCanReceiveNewActivity();

        assertThrows(com.benhsoan.domain.patient.exception.PatientInactiveException.class,
                () -> service.book(new PatientBookAppointmentCommand(
                        UUID.randomUUID(), FUTURE_DATE, START_TIME, "Kham nhi", dependentId)));

        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("P2.2: dat lich cho chinh minh cung bi chan khi ho so bi vo hieu hoa")
    void rejectsOwnBookingWhenOwnProfileIsInactive() {
        UUID userId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);

        Patient ownInactive = mock(Patient.class);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(ownInactive));
        org.mockito.Mockito.doThrow(new com.benhsoan.domain.patient.exception
                        .PatientInactiveException())
                .when(ownInactive).validateCanReceiveNewActivity();

        assertThrows(com.benhsoan.domain.patient.exception.PatientInactiveException.class,
                () -> service.book(new PatientBookAppointmentCommand(
                        UUID.randomUUID(), FUTURE_DATE, START_TIME, "Kham", null)));

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void booksAppointmentForLinkedDependentWithOnBehalfAuditContext() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);

        Patient dependent = mock(Patient.class);
        when(dependent.getId()).thenReturn(dependentId);
        when(dependent.getUserId()).thenReturn(null);
        when(patientAccessGuard.requirePatientAccess(dependentId)).thenReturn(dependent);

        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor(doctorId, roleId)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole(roleId)));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.of(schedule(doctorId, FUTURE_DATE, LocalTime.of(8, 0), LocalTime.of(17, 0), true)));
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(
                eq(doctorId), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(appointmentCodeGenerator.generate()).thenReturn("APT000200");
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientAppointmentResult result = service.book(
                new PatientBookAppointmentCommand(
                        doctorId, FUTURE_DATE, START_TIME, "Kham nhi", dependentId));

        assertEquals(dependentId, result.patientId());
        verify(patientAccessGuard).requirePatientAccess(dependentId);
        verify(patientRepository, never()).findByUserId(any());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        JsonNode node = objectMapper.readTree(captor.getValue().getDetail());
        assertEquals(dependentId.toString(), node.get("patientId").asText());
        assertEquals(userId.toString(), node.get("bookedByUserId").asText());
        org.junit.jupiter.api.Assertions.assertTrue(
                node.get("bookingOnBehalfOfDependent").asBoolean());
    }

    @Test
    void rejectsBookingWhenDependentScopeIsNotAuthorised() {
        UUID userId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientAccessGuard.requirePatientAccess(strangerId))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("denied"));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.book(new PatientBookAppointmentCommand(
                        doctorId, FUTURE_DATE, START_TIME, "Kham", strangerId)));

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void dependentBookingStillEnforcesSlotCollisionRules() {
        UUID userId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);

        Patient dependent = mock(Patient.class);
        when(dependent.getId()).thenReturn(dependentId);
        when(patientAccessGuard.requirePatientAccess(dependentId)).thenReturn(dependent);

        when(userRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor(doctorId, roleId)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole(roleId)));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, FUTURE_DATE))
                .thenReturn(Optional.of(schedule(doctorId, FUTURE_DATE, LocalTime.of(8, 0), LocalTime.of(17, 0), true)));
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(
                eq(doctorId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(mock(Appointment.class)));

        assertThrows(SlotAlreadyBookedException.class, () -> service.book(
                new PatientBookAppointmentCommand(
                        doctorId, FUTURE_DATE, START_TIME, "Kham nhi", dependentId)));
    }
}

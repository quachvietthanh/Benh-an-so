package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.enums.SlotAvailabilityStatus;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.port.dto.query.appointment.GetDoctorWeeklyScheduleTableQuery;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorDayScheduleResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorScheduleDayResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorScheduleSlotResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class GetDoctorWeeklyScheduleTableServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate MONDAY_DATE = LocalDate.of(2099, 8, 10); // Monday
    private static final LocalDate SUNDAY_DATE = LocalDate.of(2099, 8, 16); // Sunday

    private static final UUID DOCTOR_A_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID DOCTOR_B_ID = UUID.fromString("22222222-2222-2222-2222-222222222202");
    private static final UUID PATIENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock private UserRepository userRepository;
    @Mock private DoctorWeeklyScheduleRepository weeklyScheduleRepository;
    @Mock private DoctorScheduleRepository doctorScheduleRepository;
    @Mock private DoctorTimeOffRepository doctorTimeOffRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock private ClockPort clockPort;

    private GetDoctorWeeklyScheduleTableService service;

    private User doctorA;
    private User doctorB;
    private Patient patient;

    @BeforeEach
    void setUp() {
        service = new GetDoctorWeeklyScheduleTableService(
                userRepository,
                weeklyScheduleRepository,
                doctorScheduleRepository,
                doctorTimeOffRepository,
                appointmentRepository,
                patientRepository,
                clinicConfigurationRepository,
                clockPort
        );

        doctorA = User.restore(
                DOCTOR_A_ID, "bsa", "hash", "BS. Nguyen Van A",
                "a@clinic.vn", "0900000001", RoleConstants.DOCTOR, true, Instant.now(), Instant.now()
        );
        doctorB = User.restore(
                DOCTOR_B_ID, "bsb", "hash", "BS. Tran Van B",
                "b@clinic.vn", "0900000002", RoleConstants.DOCTOR, true, Instant.now(), Instant.now()
        );

        patient = Patient.create(
                "BN000001", "Nguyen Thi Banh", LocalDate.of(1990, 1, 1),
                Gender.FEMALE, "0911223344", "patient@email.com", "Hanoi",
                "123456789012", "BH123456", BloodType.O_POSITIVE,
                "Emergency", "Mother", "0911223399", true, "v1.0", UUID.randomUUID()
        );
    }

    @Test
    void tc01_returnsWeeklyTableForMultipleDoctorsWithCorrectSlots() {
        // Given: Tuesday 2099-08-11 queried -> should resolve week Monday 2099-08-10 to Sunday 2099-08-16
        LocalDate tuesday = LocalDate.of(2099, 8, 11);
        when(clockPort.now()).thenReturn(Instant.parse("2026-09-15T00:00:00Z")); // past relative to 2099

        when(userRepository.findAllActiveByRoleId(RoleConstants.DOCTOR)).thenReturn(List.of(doctorA, doctorB));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty()); // defaults 07:30 to 17:30

        // Weekly schedule: Doctor A works Monday 08:00 - 12:00; Doctor B works Monday 13:00 - 17:00
        DoctorWeeklySchedule schedA = DoctorWeeklySchedule.create(
                DOCTOR_A_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)
        );
        DoctorWeeklySchedule schedB = DoctorWeeklySchedule.create(
                DOCTOR_B_ID, DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0)
        );
        when(weeklyScheduleRepository.findActiveByDoctorIdIn(List.of(DOCTOR_A_ID, DOCTOR_B_ID)))
                .thenReturn(List.of(schedA, schedB));

        when(doctorScheduleRepository.findByDoctorIdInAndScheduleDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        // Doctor A has appointment on Monday 09:00 - 09:30
        Instant apptStart = MONDAY_DATE.atTime(9, 0).atZone(CLINIC_ZONE).toInstant();
        Instant apptEnd = MONDAY_DATE.atTime(9, 30).atZone(CLINIC_ZONE).toInstant();
        Appointment appointment = Appointment.restore(
                UUID.randomUUID(), "APT000001", patient.getId(), DOCTOR_A_ID,
                apptStart, apptEnd, AppointmentStatus.CONFIRMED, "Tai kham tim mach",
                null, null, null, UUID.randomUUID(), Instant.now()
        );
        when(appointmentRepository.findAppointmentsForDoctorsBetween(any(), any(), any(), any()))
                .thenReturn(List.of(appointment));
        when(patientRepository.findAllById(Set.of(patient.getId()))).thenReturn(List.of(patient));

        // Doctor A has time-off on Monday 10:00 - 11:00 (Nghỉ họp chuyên môn - QTN-30)
        Instant leaveStart = MONDAY_DATE.atTime(10, 0).atZone(CLINIC_ZONE).toInstant();
        Instant leaveEnd = MONDAY_DATE.atTime(11, 0).atZone(CLINIC_ZONE).toInstant();
        DoctorTimeOff timeOff = DoctorTimeOff.restore(
                UUID.randomUUID(), DOCTOR_A_ID, leaveStart, leaveEnd,
                "Hop chuyen mon khoa", TimeOffStatus.ACTIVE, UUID.randomUUID(), Instant.now(), null
        );
        when(doctorTimeOffRepository.findActiveOverlappingForDoctors(any(), any(), any()))
                .thenReturn(List.of(timeOff));

        // When: Receptionist opens weekly schedule
        DoctorWeeklyTableResult result = service.getWeeklyScheduleTable(
                new GetDoctorWeeklyScheduleTableQuery(tuesday, null)
        );

        // Then:
        assertNotNull(result);
        assertEquals(MONDAY_DATE, result.weekStartDate());
        assertEquals(SUNDAY_DATE, result.weekEndDate());
        assertEquals(2, result.doctors().size());
        assertEquals(7, result.days().size());

        // Check Monday (day 0)
        DoctorDayScheduleResult mondayResult = result.days().get(0);
        assertEquals(MONDAY_DATE, mondayResult.date());
        assertEquals(DayOfWeek.MONDAY, mondayResult.dayOfWeek());
        assertEquals(2, mondayResult.doctorSchedules().size());

        // Doctor A on Monday
        DoctorScheduleDayResult doctorASched = mondayResult.doctorSchedules().stream()
                .filter(ds -> ds.doctorId().equals(DOCTOR_A_ID))
                .findFirst().orElseThrow();
        assertTrue(doctorASched.workingDay());
        assertEquals(LocalTime.of(8, 0), doctorASched.workingStartTime());
        assertEquals(LocalTime.of(12, 0), doctorASched.workingEndTime());

        // Check slots for Doctor A
        // 07:30 - 08:00 is OFF_DUTY (before working hours)
        DoctorScheduleSlotResult slot0730 = doctorASched.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(7, 30)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.OFF_DUTY, slot0730.status());
        assertFalse(slot0730.isBookable());

        // 08:00 - 08:30 is AVAILABLE
        DoctorScheduleSlotResult slot0800 = doctorASched.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(8, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.AVAILABLE, slot0800.status());
        assertTrue(slot0800.isBookable());

        // 09:00 - 09:30 is BOOKED (contains appointment)
        DoctorScheduleSlotResult slot0900 = doctorASched.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(9, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.BOOKED, slot0900.status());
        assertFalse(slot0900.isBookable());
        assertNotNull(slot0900.appointment());
        assertEquals("APT000001", slot0900.appointment().appointmentCode());
        assertEquals("Nguyen Thi Banh", slot0900.appointment().patientName());
        assertEquals(AppointmentStatus.CONFIRMED, slot0900.appointment().status());

        // 10:00 - 10:30 is ON_LEAVE (QTN-30)
        DoctorScheduleSlotResult slot1000 = doctorASched.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(10, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.ON_LEAVE, slot1000.status());
        assertFalse(slot1000.isBookable());
        assertEquals("Hop chuyen mon khoa", slot1000.timeOffReason());

        // 12:00 - 12:30 is OFF_DUTY (after 12:00 shift end)
        DoctorScheduleSlotResult slot1200 = doctorASched.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(12, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.OFF_DUTY, slot1200.status());
        assertFalse(slot1200.isBookable());

        // Doctor B on Monday: 08:00 is OFF_DUTY, 13:00 is AVAILABLE
        DoctorScheduleDayResult doctorBSched = mondayResult.doctorSchedules().stream()
                .filter(ds -> ds.doctorId().equals(DOCTOR_B_ID))
                .findFirst().orElseThrow();
        assertTrue(doctorBSched.workingDay());
        DoctorScheduleSlotResult slotB0800 = doctorBSched.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(8, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.OFF_DUTY, slotB0800.status());

        DoctorScheduleSlotResult slotB1300 = doctorBSched.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(13, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.AVAILABLE, slotB1300.status());
        assertTrue(slotB1300.isBookable());

        // Check Tuesday for Doctor B (has no Tuesday schedule) -> workingDay = false, all slots OFF_DUTY
        DoctorDayScheduleResult tuesdayResult = result.days().get(1);
        DoctorScheduleDayResult doctorBSchedTue = tuesdayResult.doctorSchedules().stream()
                .filter(ds -> ds.doctorId().equals(DOCTOR_B_ID))
                .findFirst().orElseThrow();
        assertFalse(doctorBSchedTue.workingDay());
        assertTrue(doctorBSchedTue.slots().stream().allMatch(s -> s.status() == SlotAvailabilityStatus.OFF_DUTY));
    }

    @Test
    void filtersBySingleDoctorWhenDoctorIdSpecified() {
        when(clockPort.now()).thenReturn(Instant.parse("2026-09-15T00:00:00Z"));
        when(userRepository.findById(DOCTOR_A_ID)).thenReturn(Optional.of(doctorA));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
        when(weeklyScheduleRepository.findActiveByDoctorIdIn(List.of(DOCTOR_A_ID))).thenReturn(List.of());
        when(doctorScheduleRepository.findByDoctorIdInAndScheduleDateBetween(any(), any(), any())).thenReturn(List.of());
        when(doctorTimeOffRepository.findActiveOverlappingForDoctors(any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findAppointmentsForDoctorsBetween(any(), any(), any(), any())).thenReturn(List.of());

        DoctorWeeklyTableResult result = service.getWeeklyScheduleTable(
                new GetDoctorWeeklyScheduleTableQuery(MONDAY_DATE, DOCTOR_A_ID)
        );

        assertNotNull(result);
        assertEquals(1, result.doctors().size());
        assertEquals(DOCTOR_A_ID, result.doctors().get(0).id());
        assertEquals(1, result.days().get(0).doctorSchedules().size());
        assertEquals(DOCTOR_A_ID, result.days().get(0).doctorSchedules().get(0).doctorId());
    }

    @Test
    void throwsDoctorNotFoundExceptionWhenDoctorNotFoundOrInactive() {
        when(clockPort.now()).thenReturn(Instant.parse("2026-09-15T00:00:00Z"));
        when(userRepository.findById(DOCTOR_A_ID)).thenReturn(Optional.empty());

        assertThrows(DoctorNotFoundException.class, () ->
                service.getWeeklyScheduleTable(new GetDoctorWeeklyScheduleTableQuery(MONDAY_DATE, DOCTOR_A_ID))
        );
    }

    @Test
    void marksPastSlotsAsPastAndNotBookable() {
        // Today is Monday 2099-08-10 at 09:15:00
        Instant now = MONDAY_DATE.atTime(9, 15).atZone(CLINIC_ZONE).toInstant();
        when(clockPort.now()).thenReturn(now);

        when(userRepository.findById(DOCTOR_A_ID)).thenReturn(Optional.of(doctorA));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());

        DoctorWeeklySchedule sched = DoctorWeeklySchedule.create(
                DOCTOR_A_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)
        );
        when(weeklyScheduleRepository.findActiveByDoctorIdIn(List.of(DOCTOR_A_ID))).thenReturn(List.of(sched));
        when(doctorScheduleRepository.findByDoctorIdInAndScheduleDateBetween(any(), any(), any())).thenReturn(List.of());
        when(doctorTimeOffRepository.findActiveOverlappingForDoctors(any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findAppointmentsForDoctorsBetween(any(), any(), any(), any())).thenReturn(List.of());

        DoctorWeeklyTableResult result = service.getWeeklyScheduleTable(
                new GetDoctorWeeklyScheduleTableQuery(MONDAY_DATE, DOCTOR_A_ID)
        );

        DoctorScheduleDayResult monday = result.days().get(0).doctorSchedules().get(0);

        // 08:00 - 08:30 is in the past -> PAST
        DoctorScheduleSlotResult slot0800 = monday.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(8, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.PAST, slot0800.status());
        assertFalse(slot0800.isBookable());

        // 08:30 - 09:00 is in the past -> PAST
        DoctorScheduleSlotResult slot0830 = monday.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(8, 30)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.PAST, slot0830.status());
        assertFalse(slot0830.isBookable());

        // 09:00 - 09:30 is current/past slot start (09:00 is before 09:15) -> PAST
        DoctorScheduleSlotResult slot0900 = monday.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(9, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.PAST, slot0900.status());

        // 09:30 - 10:00 starts at 09:30 which is after 09:15 -> AVAILABLE
        DoctorScheduleSlotResult slot0930 = monday.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(9, 30)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.AVAILABLE, slot0930.status());
        assertTrue(slot0930.isBookable());
    }

    @Test
    void specificDateScheduleOverridesWeeklySchedule() {
        when(clockPort.now()).thenReturn(Instant.parse("2026-09-15T00:00:00Z"));
        when(userRepository.findById(DOCTOR_A_ID)).thenReturn(Optional.of(doctorA));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());

        // Weekly schedule: 08:00 - 12:00
        DoctorWeeklySchedule weekly = DoctorWeeklySchedule.create(
                DOCTOR_A_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)
        );
        when(weeklyScheduleRepository.findActiveByDoctorIdIn(List.of(DOCTOR_A_ID))).thenReturn(List.of(weekly));

        // Date override: 13:00 - 17:00
        DoctorSchedule dateOverride = DoctorSchedule.create(
                DOCTOR_A_ID, MONDAY_DATE, LocalTime.of(13, 0), LocalTime.of(17, 0)
        );
        when(doctorScheduleRepository.findByDoctorIdInAndScheduleDateBetween(any(), any(), any()))
                .thenReturn(List.of(dateOverride));
        when(doctorTimeOffRepository.findActiveOverlappingForDoctors(any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findAppointmentsForDoctorsBetween(any(), any(), any(), any())).thenReturn(List.of());

        DoctorWeeklyTableResult result = service.getWeeklyScheduleTable(
                new GetDoctorWeeklyScheduleTableQuery(MONDAY_DATE, DOCTOR_A_ID)
        );

        DoctorScheduleDayResult monday = result.days().get(0).doctorSchedules().get(0);
        assertTrue(monday.workingDay());
        assertEquals(LocalTime.of(13, 0), monday.workingStartTime());
        assertEquals(LocalTime.of(17, 0), monday.workingEndTime());

        // 08:00 is now OFF_DUTY
        DoctorScheduleSlotResult slot0800 = monday.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(8, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.OFF_DUTY, slot0800.status());

        // 13:00 is now AVAILABLE
        DoctorScheduleSlotResult slot1300 = monday.slots().stream()
                .filter(s -> s.slotStartTime().equals(LocalTime.of(13, 0)))
                .findFirst().orElseThrow();
        assertEquals(SlotAvailabilityStatus.AVAILABLE, slot1300.status());
        assertTrue(slot1300.isBookable());
    }
}

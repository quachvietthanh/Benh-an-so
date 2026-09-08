package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.exception.DoctorNotWorkingException;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;

class DoctorScheduleResolutionServiceTest {

    private DoctorScheduleRepository doctorScheduleRepository;
    private DoctorWeeklyScheduleRepository doctorWeeklyScheduleRepository;
    private DoctorTimeOffRepository doctorTimeOffRepository;
    private DoctorScheduleResolutionService service;

    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @BeforeEach
    void setUp() {
        doctorScheduleRepository = mock(DoctorScheduleRepository.class);
        doctorWeeklyScheduleRepository = mock(DoctorWeeklyScheduleRepository.class);
        doctorTimeOffRepository = mock(DoctorTimeOffRepository.class);

        service = new DoctorScheduleResolutionService(
                doctorScheduleRepository,
                doctorWeeklyScheduleRepository,
                doctorTimeOffRepository
        );
    }

    @Test
    void validateDoctorWorkingAndAvailable_SuccessWhenWithinDateScheduleAndNoTimeOff() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        Instant start = date.atTime(9, 0).atZone(ZONE).toInstant();
        Instant end = date.atTime(9, 30).atZone(ZONE).toInstant();

        DoctorSchedule dateSchedule = DoctorSchedule.create(DOCTOR_ID, date, LocalTime.of(8, 0), LocalTime.of(17, 0));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, date)).thenReturn(Optional.of(dateSchedule));
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(DOCTOR_ID, start, end)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
    }

    @Test
    void validateDoctorWorkingAndAvailable_ThrowsWhenDoctorOnTimeOff() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        Instant start = date.atTime(9, 0).atZone(ZONE).toInstant();
        Instant end = date.atTime(9, 30).atZone(ZONE).toInstant();

        DoctorSchedule dateSchedule = DoctorSchedule.create(DOCTOR_ID, date, LocalTime.of(8, 0), LocalTime.of(17, 0));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, date)).thenReturn(Optional.of(dateSchedule));

        DoctorTimeOff timeOff = mock(DoctorTimeOff.class);
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(DOCTOR_ID, start, end)).thenReturn(List.of(timeOff));

        DoctorNotWorkingException ex = assertThrows(DoctorNotWorkingException.class,
                () -> service.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
        assertEquals("Bác sĩ không làm việc trong khung giờ này.", ex.getMessage());
    }

    @Test
    void validateDoctorWorkingAndAvailable_FallsBackToWeeklyScheduleWhenNoDateSchedule() {
        // 2026-09-10 is a THURSDAY
        LocalDate date = LocalDate.of(2026, 9, 10);
        assertEquals(DayOfWeek.THURSDAY, date.getDayOfWeek());

        Instant start = date.atTime(10, 0).atZone(ZONE).toInstant();
        Instant end = date.atTime(10, 30).atZone(ZONE).toInstant();

        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, date)).thenReturn(Optional.empty());

        DoctorWeeklySchedule weekly = DoctorWeeklySchedule.create(
                DOCTOR_ID, DayOfWeek.THURSDAY, LocalTime.of(8, 30), LocalTime.of(16, 30)
        );
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DayOfWeek.THURSDAY))
                .thenReturn(List.of(weekly));
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(DOCTOR_ID, start, end)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
    }

    @Test
    void validateDoctorWorkingAndAvailable_ThrowsWhenOutsideWeeklySchedule() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        Instant start = date.atTime(17, 0).atZone(ZONE).toInstant();
        Instant end = date.atTime(17, 30).atZone(ZONE).toInstant();

        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, date)).thenReturn(Optional.empty());

        DoctorWeeklySchedule weekly = DoctorWeeklySchedule.create(
                DOCTOR_ID, DayOfWeek.THURSDAY, LocalTime.of(8, 30), LocalTime.of(16, 30)
        );
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DayOfWeek.THURSDAY))
                .thenReturn(List.of(weekly));

        DoctorNotWorkingException ex = assertThrows(DoctorNotWorkingException.class,
                () -> service.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
        assertEquals("Bác sĩ không làm việc trong khung giờ này.", ex.getMessage());
    }

    @Test
    void validateDoctorWorkingAndAvailable_MultipleShiftsSameDay_SuccessWhenInAnyShift() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        DoctorWeeklySchedule morningShift = DoctorWeeklySchedule.create(
                DOCTOR_ID, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), true
        );
        DoctorWeeklySchedule afternoonShift = DoctorWeeklySchedule.create(
                DOCTOR_ID, DayOfWeek.THURSDAY, LocalTime.of(13, 30), LocalTime.of(17, 0), true
        );

        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, date)).thenReturn(Optional.empty());
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DayOfWeek.THURSDAY))
                .thenReturn(List.of(morningShift, afternoonShift));

        // In morning shift: 09:00 - 09:30
        Instant morningStart = date.atTime(9, 0).atZone(ZONE).toInstant();
        Instant morningEnd = date.atTime(9, 30).atZone(ZONE).toInstant();
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(DOCTOR_ID, morningStart, morningEnd)).thenReturn(List.of());
        assertDoesNotThrow(() -> service.validateDoctorWorkingAndAvailable(DOCTOR_ID, morningStart, morningEnd));

        // In afternoon shift: 14:00 - 14:30
        Instant afternoonStart = date.atTime(14, 0).atZone(ZONE).toInstant();
        Instant afternoonEnd = date.atTime(14, 30).atZone(ZONE).toInstant();
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(DOCTOR_ID, afternoonStart, afternoonEnd)).thenReturn(List.of());
        assertDoesNotThrow(() -> service.validateDoctorWorkingAndAvailable(DOCTOR_ID, afternoonStart, afternoonEnd));

        // In-between shifts (lunch break): 12:15 - 12:45 -> throws
        Instant lunchStart = date.atTime(12, 15).atZone(ZONE).toInstant();
        Instant lunchEnd = date.atTime(12, 45).atZone(ZONE).toInstant();
        DoctorNotWorkingException ex = assertThrows(DoctorNotWorkingException.class,
                () -> service.validateDoctorWorkingAndAvailable(DOCTOR_ID, lunchStart, lunchEnd));
        assertEquals("Bác sĩ không làm việc trong khung giờ này.", ex.getMessage());
    }

    @Test
    void validateDoctorWorkingAndAvailable_ThrowsWhenShiftIsInactive() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        DoctorWeeklySchedule inactiveShift = DoctorWeeklySchedule.create(
                DOCTOR_ID, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), false
        );

        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, date)).thenReturn(Optional.empty());
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DayOfWeek.THURSDAY))
                .thenReturn(List.of(inactiveShift));

        Instant start = date.atTime(9, 0).atZone(ZONE).toInstant();
        Instant end = date.atTime(9, 30).atZone(ZONE).toInstant();

        DoctorNotWorkingException ex = assertThrows(DoctorNotWorkingException.class,
                () -> service.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
        assertEquals("Bác sĩ không làm việc trong khung giờ này.", ex.getMessage());
    }
}

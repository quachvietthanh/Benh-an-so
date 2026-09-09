package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.exception.DoctorNotWorkingException;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;

@ExtendWith(MockitoExtension.class)
class DoctorScheduleValidatorTest {

    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");

    @Mock private DoctorScheduleRepository doctorScheduleRepository;
    @Mock private DoctorWeeklyScheduleRepository weeklyScheduleRepository;
    @Mock private DoctorTimeOffRepository doctorTimeOffRepository;

    private DoctorScheduleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DoctorScheduleValidator(
                doctorScheduleRepository,
                weeklyScheduleRepository,
                doctorTimeOffRepository
        );
    }

    private void mockDoctorWorkingOnMonday(LocalTime start, LocalTime end) {
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(eq(DOCTOR_ID), any())).thenReturn(Optional.empty());
        DoctorWeeklySchedule mondaySchedule = DoctorWeeklySchedule.create(
                DOCTOR_ID, DayOfWeek.MONDAY, start, end, NOW
        );
        when(weeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(mondaySchedule));
    }

    @Test
    void allowsValidSameDayAppointmentWithinWorkingHours() {
        mockDoctorWorkingOnMonday(LocalTime.of(8, 0), LocalTime.of(17, 0));
        when(doctorTimeOffRepository.existsActiveOverlapping(eq(DOCTOR_ID), any(), any())).thenReturn(false);

        // Monday 2026-09-14 09:00 -> 09:30 UTC+7
        Instant start = ZonedDateTime.of(2026, 9, 14, 9, 0, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();
        Instant end = ZonedDateTime.of(2026, 9, 14, 9, 30, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();

        assertDoesNotThrow(() -> validator.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
    }

    @Test
    void rejectsCrossDayAppointmentCrossingMidnight() {
        // Monday 2026-09-14 08:00 -> Tuesday 2026-09-15 09:00 UTC+7 (Finding [P2-2])
        Instant start = ZonedDateTime.of(2026, 9, 14, 8, 0, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();
        Instant end = ZonedDateTime.of(2026, 9, 15, 9, 0, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();

        DoctorNotWorkingException ex = assertThrows(DoctorNotWorkingException.class,
                () -> validator.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
        assertEquals("Khung giờ đặt lịch nằm ngoài giờ làm việc của bác sĩ.", ex.getMessage());
    }

    @Test
    void rejectsAppointmentStartingBeforeWorkingHours() {
        mockDoctorWorkingOnMonday(LocalTime.of(8, 0), LocalTime.of(17, 0));

        // Monday 2026-09-14 07:59 -> 08:30 UTC+7
        Instant start = ZonedDateTime.of(2026, 9, 14, 7, 59, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();
        Instant end = ZonedDateTime.of(2026, 9, 14, 8, 30, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();

        DoctorNotWorkingException ex = assertThrows(DoctorNotWorkingException.class,
                () -> validator.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
        assertEquals("Khung giờ đặt lịch nằm ngoài giờ làm việc của bác sĩ.", ex.getMessage());
    }

    @Test
    void rejectsAppointmentEndingAfterWorkingHours() {
        mockDoctorWorkingOnMonday(LocalTime.of(8, 0), LocalTime.of(17, 0));

        // Monday 2026-09-14 16:30 -> 17:01 UTC+7
        Instant start = ZonedDateTime.of(2026, 9, 14, 16, 30, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();
        Instant end = ZonedDateTime.of(2026, 9, 14, 17, 1, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();

        DoctorNotWorkingException ex = assertThrows(DoctorNotWorkingException.class,
                () -> validator.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
        assertEquals("Khung giờ đặt lịch nằm ngoài giờ làm việc của bác sĩ.", ex.getMessage());
    }

    @Test
    void rejectsWhenDoctorHasActiveTimeOff() {
        mockDoctorWorkingOnMonday(LocalTime.of(8, 0), LocalTime.of(17, 0));

        Instant start = ZonedDateTime.of(2026, 9, 14, 10, 0, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();
        Instant end = ZonedDateTime.of(2026, 9, 14, 10, 30, 0, 0, DoctorScheduleValidator.CLINIC_ZONE).toInstant();

        when(doctorTimeOffRepository.existsActiveOverlapping(DOCTOR_ID, start, end)).thenReturn(true);

        DoctorNotWorkingException ex = assertThrows(DoctorNotWorkingException.class,
                () -> validator.validateDoctorWorkingAndAvailable(DOCTOR_ID, start, end));
        assertEquals("Bác sĩ không làm việc trong khung giờ này.", ex.getMessage());
    }
}

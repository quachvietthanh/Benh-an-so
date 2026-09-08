package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.port.dto.query.appointment.GetDoctorAvailableSlotsQuery;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class GetDoctorAvailableSlotsServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2099, 8, 10); // Monday

    @Mock private DoctorScheduleRepository doctorScheduleRepository;
    @Mock private DoctorWeeklyScheduleRepository doctorWeeklyScheduleRepository;
    @Mock private DoctorTimeOffRepository doctorTimeOffRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ClockPort clockPort;

    private GetDoctorAvailableSlotsService service;

    @BeforeEach
    void setUp() {
        service = new GetDoctorAvailableSlotsService(
                doctorScheduleRepository,
                doctorWeeklyScheduleRepository,
                doctorTimeOffRepository,
                appointmentRepository,
                clockPort
        );
    }

    @Test
    void returnsEmptyWhenNoScheduleExists() {
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                .thenReturn(Optional.empty());
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DATE.getDayOfWeek()))
                .thenReturn(List.of());

        assertTrue(service.getAvailableSlots(new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE)).isEmpty());
    }

    @Test
    void computesAvailableSlotsSubtractingBookedOnes() {
        DoctorSchedule schedule = DoctorSchedule.create(
                DOCTOR_ID, DATE, LocalTime.of(8, 0), LocalTime.of(10, 0));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                .thenReturn(Optional.of(schedule));
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-26T02:00:00Z"));

        Instant slot3Start = DATE.atTime(9, 0).atZone(CLINIC_ZONE).toInstant();
        Instant slot3End = DATE.atTime(9, 30).atZone(CLINIC_ZONE).toInstant();
        Appointment booked = Appointment.restore(
                UUID.randomUUID(), "APT1", PATIENT_ID, DOCTOR_ID,
                slot3Start, slot3End, AppointmentStatus.SCHEDULED, "Kham tong quat",
                null, null, null, UUID.randomUUID(), Instant.now());

        Instant dayStart = DATE.atTime(8, 0).atZone(CLINIC_ZONE).toInstant();
        Instant dayEnd = DATE.atTime(10, 0).atZone(CLINIC_ZONE).toInstant();
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(DOCTOR_ID, dayStart, dayEnd))
                .thenReturn(List.of(booked));
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(eq(DOCTOR_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        List<DoctorAvailableSlotResult> slots = service.getAvailableSlots(
                new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE));

        assertEquals(4, slots.size());
        assertEquals(3, slots.stream().filter(DoctorAvailableSlotResult::isAvailable).count());
        assertEquals(1, slots.stream().filter(slot -> !slot.isAvailable()).count());
    }

    @Test
    void filtersOutSlotsStrictlyInThePast() {
        DoctorSchedule schedule = DoctorSchedule.create(
                DOCTOR_ID, DATE, LocalTime.of(8, 0), LocalTime.of(10, 0));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                .thenReturn(Optional.of(schedule));
        when(clockPort.now()).thenReturn(DATE.atTime(9, 15).atZone(CLINIC_ZONE).toInstant());
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(
                any(UUID.class), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(eq(DOCTOR_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        List<DoctorAvailableSlotResult> slots = service.getAvailableSlots(
                new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE));

        assertEquals(1, slots.size());
        assertEquals(DATE.atTime(9, 30).atZone(CLINIC_ZONE).toInstant(), slots.get(0).startTime());
        assertTrue(slots.get(0).isAvailable());
    }

    @Test
    void computesSlotsFromWeeklyScheduleWhenDateScheduleIsMissing() {
        // Fallback to recurring weekly schedule
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                .thenReturn(Optional.empty());
        DoctorWeeklySchedule weekly = DoctorWeeklySchedule.restore(
                UUID.randomUUID(), DOCTOR_ID, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), true, Instant.now(), null
        );
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(List.of(weekly));
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-26T02:00:00Z"));
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(eq(DOCTOR_ID), any(), any()))
                .thenReturn(List.of());
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(eq(DOCTOR_ID), any(), any()))
                .thenReturn(List.of());

        List<DoctorAvailableSlotResult> slots = service.getAvailableSlots(
                new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE));

        assertEquals(2, slots.size());
        assertTrue(slots.get(0).isAvailable());
        assertTrue(slots.get(1).isAvailable());
    }

    @Test
    void computesSlotsFromMultipleWeeklyShiftsInSameDay() {
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                .thenReturn(Optional.empty());
        DoctorWeeklySchedule morning = DoctorWeeklySchedule.restore(
                UUID.randomUUID(), DOCTOR_ID, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(10, 0), true, Instant.now(), null
        );
        DoctorWeeklySchedule afternoon = DoctorWeeklySchedule.restore(
                UUID.randomUUID(), DOCTOR_ID, DayOfWeek.MONDAY,
                LocalTime.of(13, 30), LocalTime.of(15, 0), true, Instant.now(), null
        );
        when(doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(List.of(morning, afternoon));
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-26T02:00:00Z"));
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(eq(DOCTOR_ID), any(), any()))
                .thenReturn(List.of());
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(eq(DOCTOR_ID), any(), any()))
                .thenReturn(List.of());

        List<DoctorAvailableSlotResult> slots = service.getAvailableSlots(
                new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE));

        // Morning: 08:00, 08:30, 09:00, 09:30 (4 slots)
        // Afternoon: 13:30, 14:00, 14:30 (3 slots)
        assertEquals(7, slots.size());
        assertEquals(7, slots.stream().filter(DoctorAvailableSlotResult::isAvailable).count());
    }

    @Test
    void marksSlotsUnavailableWhenDoctorHasTimeOff() {
        // NCL-03-CN-006 TC-05: patient portal available slots excludes / marks unavailable doctor time-offs
        DoctorSchedule schedule = DoctorSchedule.create(
                DOCTOR_ID, DATE, LocalTime.of(8, 0), LocalTime.of(10, 0));
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                .thenReturn(Optional.of(schedule));
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-26T02:00:00Z"));

        Instant timeOffStart = DATE.atTime(8, 30).atZone(CLINIC_ZONE).toInstant();
        Instant timeOffEnd = DATE.atTime(9, 30).atZone(CLINIC_ZONE).toInstant();
        DoctorTimeOff timeOff = DoctorTimeOff.restore(
                UUID.randomUUID(), DOCTOR_ID, timeOffStart, timeOffEnd,
                "Hop khoa phong", TimeOffStatus.ACTIVE, UUID.randomUUID(), Instant.now(), null
        );

        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(eq(DOCTOR_ID), any(), any()))
                .thenReturn(List.of());
        when(doctorTimeOffRepository.findOverlappingActiveTimeOffs(eq(DOCTOR_ID), any(), any()))
                .thenReturn(List.of(timeOff));

        List<DoctorAvailableSlotResult> slots = service.getAvailableSlots(
                new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE));

        assertEquals(4, slots.size());
        // 08:00 - 08:30: available
        assertTrue(slots.get(0).isAvailable());
        // 08:30 - 09:00: unavailable (time-off)
        assertFalse(slots.get(1).isAvailable());
        // 09:00 - 09:30: unavailable (time-off)
        assertFalse(slots.get(2).isAvailable());
        // 09:30 - 10:00: available
        assertTrue(slots.get(3).isAvailable());
    }
}

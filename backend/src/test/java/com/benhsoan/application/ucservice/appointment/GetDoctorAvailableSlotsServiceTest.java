package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.query.appointment.GetDoctorAvailableSlotsQuery;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class GetDoctorAvailableSlotsServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2099, 8, 10);

    @Mock private DoctorScheduleRepository doctorScheduleRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ClockPort clockPort;

    private GetDoctorAvailableSlotsService service;

    @BeforeEach
    void setUp() {
        service = new GetDoctorAvailableSlotsService(
                doctorScheduleRepository,
                appointmentRepository,
                clockPort
        );
    }

    @Test
    void returnsEmptyWhenNoScheduleExists() {
        when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                .thenReturn(Optional.empty());

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

        List<DoctorAvailableSlotResult> slots = service.getAvailableSlots(
                new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE));

        assertEquals(1, slots.size());
        assertEquals(DATE.atTime(9, 30).atZone(CLINIC_ZONE).toInstant(), slots.get(0).startTime());
        assertTrue(slots.get(0).isAvailable());
    }
}

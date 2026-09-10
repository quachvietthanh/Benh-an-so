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
import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
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

        @Mock
        private DoctorScheduleRepository doctorScheduleRepository;
        @Mock
        private com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository weeklyScheduleRepository;
        @Mock
        private com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository doctorTimeOffRepository;
        @Mock
        private AppointmentRepository appointmentRepository;
        @Mock
        private ClockPort clockPort;

        private GetDoctorAvailableSlotsService service;

        @BeforeEach
        void setUp() {
                service = new GetDoctorAvailableSlotsService(
                                doctorScheduleRepository,
                                weeklyScheduleRepository,
                                doctorTimeOffRepository,
                                appointmentRepository,
                                clockPort);
        }

        @Test
        void returnsEmptyWhenNoScheduleExists() {
                when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                                .thenReturn(Optional.empty());
                when(weeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DATE.getDayOfWeek()))
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

        @Test
        void computesAvailableSlotsUsingWeeklyScheduleWhenNoDateScheduleExists() {
                when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                                .thenReturn(Optional.empty());

                Instant now = Instant.parse("2026-08-26T02:00:00Z");
                DoctorWeeklySchedule weeklySchedule = DoctorWeeklySchedule.create(
                                DOCTOR_ID, DATE.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(9, 0), now);
                when(weeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DATE.getDayOfWeek()))
                                .thenReturn(Optional.of(weeklySchedule));
                when(clockPort.now()).thenReturn(now);
                when(appointmentRepository.findActiveAppointmentsForDoctorBetween(any(), any(), any()))
                                .thenReturn(List.of());
                when(doctorTimeOffRepository.findActiveOverlapping(any(), any(), any()))
                                .thenReturn(List.of());

                List<DoctorAvailableSlotResult> slots = service.getAvailableSlots(
                                new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE));

                assertEquals(2, slots.size());
                assertTrue(slots.stream().allMatch(DoctorAvailableSlotResult::isAvailable));
        }

        @Test
        void filtersOutSlotsOverlappingWithActiveDoctorTimeOff() {
                // TC-05: Khung giờ nghỉ không còn hiển thị trống
                DoctorSchedule schedule = DoctorSchedule.create(
                                DOCTOR_ID, DATE, LocalTime.of(8, 0), LocalTime.of(10, 0));
                when(doctorScheduleRepository.findByDoctorIdAndScheduleDate(DOCTOR_ID, DATE))
                                .thenReturn(Optional.of(schedule));
                when(clockPort.now()).thenReturn(Instant.parse("2026-08-26T02:00:00Z"));
                when(appointmentRepository.findActiveAppointmentsForDoctorBetween(any(), any(), any()))
                                .thenReturn(List.of());

                // Time off from 08:30 to 09:30
                Instant timeOffStart = DATE.atTime(8, 30).atZone(CLINIC_ZONE).toInstant();
                Instant timeOffEnd = DATE.atTime(9, 30).atZone(CLINIC_ZONE).toInstant();
                DoctorTimeOff timeOff = DoctorTimeOff.restore(
                                UUID.randomUUID(), DOCTOR_ID, timeOffStart, timeOffEnd, "Bận việc",
                                TimeOffStatus.ACTIVE, UUID.randomUUID(), Instant.now(), null);
                when(doctorTimeOffRepository.findActiveOverlapping(any(), any(), any()))
                                .thenReturn(List.of(timeOff));

                List<DoctorAvailableSlotResult> slots = service.getAvailableSlots(
                                new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE));

                assertEquals(4, slots.size());
                // 08:00 - 08:30: available
                assertTrue(slots.get(0).isAvailable());
                // 08:30 - 09:00: not available due to time off
                assertEquals(false, slots.get(1).isAvailable());
                // 09:00 - 09:30: not available due to time off
                assertEquals(false, slots.get(2).isAvailable());
                // 09:30 - 10:00: available
                assertTrue(slots.get(3).isAvailable());
        }

        @Test
        void returnsEmptyWhenDayIsDeactivatedInWeeklySchedule() {
                Instant now = Instant.parse("2026-08-26T02:00:00Z");
                DoctorWeeklySchedule weeklyDisabled = DoctorWeeklySchedule.create(
                                DOCTOR_ID, DATE.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(12, 0), now);
                weeklyDisabled.update(LocalTime.of(8, 0), LocalTime.of(12, 0), false, now);

                when(weeklyScheduleRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, DATE.getDayOfWeek()))
                                .thenReturn(Optional.of(weeklyDisabled));

                List<DoctorAvailableSlotResult> slots = service.getAvailableSlots(
                                new GetDoctorAvailableSlotsQuery(DOCTOR_ID, DATE));

                assertTrue(slots.isEmpty());
        }
}

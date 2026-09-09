package com.benhsoan.application.ucservice.appointment;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.port.dto.query.appointment.GetDoctorAvailableSlotsQuery;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;
import com.benhsoan.port.inbound.appointment.GetDoctorAvailableSlotsUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-003 / NCL-03-CN-006 (TC-05 / QTN-30): computes available 30-minute slots for a doctor on a date
 * by resolving the doctor's schedule (date-specific or weekly recurring), subtracting active bookings,
 * subtracting active time-offs (leaves), and dropping past slots.
 */
@Service
@RequiredArgsConstructor
public class GetDoctorAvailableSlotsService implements GetDoctorAvailableSlotsUseCase {

    private static final Duration SLOT_DURATION = Duration.ofMinutes(30);

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorScheduleRepository doctorScheduleRepository;

    private final DoctorWeeklyScheduleRepository weeklyScheduleRepository;

    private final DoctorTimeOffRepository doctorTimeOffRepository;

    private final AppointmentRepository appointmentRepository;

    private final ClockPort clockPort;

    @Override
    public List<DoctorAvailableSlotResult> getAvailableSlots(GetDoctorAvailableSlotsQuery query) {
        Optional<DoctorSchedule> dateSchedule = doctorScheduleRepository
                .findByDoctorIdAndScheduleDate(query.doctorId(), query.date());

        if (dateSchedule.isPresent()) {
            DoctorSchedule schedule = dateSchedule.get();
            if (!schedule.isActive()) {
                return List.of();
            }
            return computeSlots(query.doctorId(), query.date(), schedule.getStartTime(), schedule.getEndTime(), clockPort.now());
        }

        // Fallback to weekly schedule
        Optional<DoctorWeeklySchedule> weeklySchedule = weeklyScheduleRepository
                .findByDoctorIdAndDayOfWeek(query.doctorId(), query.date().getDayOfWeek());
        if (weeklySchedule.isPresent()) {
            DoctorWeeklySchedule ws = weeklySchedule.get();
            if (!ws.isActive()) {
                return List.of();
            }
            return computeSlots(query.doctorId(), query.date(), ws.getStartTime(), ws.getEndTime(), clockPort.now());
        }

        return List.of();
    }

    private List<DoctorAvailableSlotResult> computeSlots(
            UUID doctorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Instant now
    ) {
        Instant scheduleStart = date
                .atTime(startTime)
                .atZone(CLINIC_ZONE)
                .toInstant();

        Instant scheduleEnd = date
                .atTime(endTime)
                .atZone(CLINIC_ZONE)
                .toInstant();

        List<Appointment> booked = appointmentRepository.findActiveAppointmentsForDoctorBetween(
                doctorId,
                scheduleStart,
                scheduleEnd
        );

        List<DoctorTimeOff> timeOffs = doctorTimeOffRepository.findActiveOverlapping(
                doctorId,
                scheduleStart,
                scheduleEnd
        );

        List<DoctorAvailableSlotResult> slots = new ArrayList<>();

        Instant slotStart = scheduleStart;
        while (slotStart.isBefore(scheduleEnd)) {
            Instant slotEnd = slotStart.plus(SLOT_DURATION);

            if (slotEnd.isAfter(scheduleEnd)) {
                break;
            }

            if (!slotStart.isBefore(now)) {
                Instant currentStart = slotStart;
                Instant currentEnd = slotEnd;

                boolean isBooked = booked.stream()
                        .anyMatch(appointment -> overlaps(appointment, currentStart, currentEnd));

                boolean isOnLeave = timeOffs.stream()
                        .anyMatch(timeOff -> timeOff.overlaps(currentStart, currentEnd));

                boolean isAvailable = !isBooked && !isOnLeave;
                slots.add(new DoctorAvailableSlotResult(currentStart, currentEnd, isAvailable));
            }

            slotStart = slotEnd;
        }

        return slots;
    }

    private boolean overlaps(Appointment appointment, Instant start, Instant end) {
        return appointment.getStartTime().isBefore(end)
                && appointment.getEndTime().isAfter(start);
    }
}

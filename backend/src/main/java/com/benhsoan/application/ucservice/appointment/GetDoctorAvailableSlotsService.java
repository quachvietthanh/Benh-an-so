package com.benhsoan.application.ucservice.appointment;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.port.dto.query.appointment.GetDoctorAvailableSlotsQuery;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;
import com.benhsoan.port.inbound.appointment.GetDoctorAvailableSlotsUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-003 CV-01 / CV-02: computes available 30-minute slots for a doctor on a date
 * by resolving the doctor's schedule, subtracting active bookings and dropping past slots.
 */
@Service
@RequiredArgsConstructor
public class GetDoctorAvailableSlotsService implements GetDoctorAvailableSlotsUseCase {

    private static final Duration SLOT_DURATION = Duration.ofMinutes(30);

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorScheduleRepository doctorScheduleRepository;

    private final AppointmentRepository appointmentRepository;

    private final ClockPort clockPort;

    @Override
    public List<DoctorAvailableSlotResult> getAvailableSlots(GetDoctorAvailableSlotsQuery query) {
        return doctorScheduleRepository
                .findByDoctorIdAndScheduleDate(query.doctorId(), query.date())
                .filter(DoctorSchedule::isActive)
                .map(schedule -> computeSlots(schedule, clockPort.now()))
                .orElseGet(List::of);
    }

    private List<DoctorAvailableSlotResult> computeSlots(DoctorSchedule schedule, Instant now) {
        Instant scheduleStart = schedule.getScheduleDate()
                .atTime(schedule.getStartTime())
                .atZone(CLINIC_ZONE)
                .toInstant();

        Instant scheduleEnd = schedule.getScheduleDate()
                .atTime(schedule.getEndTime())
                .atZone(CLINIC_ZONE)
                .toInstant();

        List<Appointment> booked = appointmentRepository.findActiveAppointmentsForDoctorBetween(
                schedule.getDoctorId(),
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
                boolean isAvailable = booked.stream()
                        .noneMatch(appointment -> overlaps(appointment, currentStart, currentEnd));
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

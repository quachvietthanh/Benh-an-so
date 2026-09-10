package com.benhsoan.application.ucservice.appointment;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.exception.DoctorNotWorkingException;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Validates that requested appointment intervals fall strictly within a doctor's active working schedule
 * (either specific date override or weekly recurring schedule) and do not overlap with active time-offs (QTN-30).
 */
@Component
@RequiredArgsConstructor
public class DoctorScheduleValidator {

    public static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorWeeklyScheduleRepository weeklyScheduleRepository;
    private final DoctorTimeOffRepository doctorTimeOffRepository;

    public record EffectiveWorkingHours(LocalTime startTime, LocalTime endTime) {}

    public Optional<EffectiveWorkingHours> resolveWorkingHours(UUID doctorId, LocalDate date) {
        // 1. Recurring weekly schedule takes precedence as the base schedule
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Optional<DoctorWeeklySchedule> weeklySchedule = weeklyScheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        if (weeklySchedule.isPresent()) {
            DoctorWeeklySchedule ws = weeklySchedule.get();
            if (!ws.isActive()) {
                return Optional.empty();
            }
            // If active in weekly schedule, check for specific date override in doctor_schedules
            Optional<DoctorSchedule> dateSchedule = doctorScheduleRepository.findByDoctorIdAndScheduleDate(doctorId, date);
            if (dateSchedule.isPresent()) {
                DoctorSchedule ds = dateSchedule.get();
                if (!ds.isActive()) {
                    return Optional.empty();
                }
                return Optional.of(new EffectiveWorkingHours(ds.getStartTime(), ds.getEndTime()));
            }
            return Optional.of(new EffectiveWorkingHours(ws.getStartTime(), ws.getEndTime()));
        }

        // 2. Fallback to specific date schedule for doctors without weekly schedule configured
        Optional<DoctorSchedule> dateSchedule = doctorScheduleRepository.findByDoctorIdAndScheduleDate(doctorId, date);
        if (dateSchedule.isPresent()) {
            DoctorSchedule ds = dateSchedule.get();
            if (!ds.isActive()) {
                return Optional.empty();
            }
            return Optional.of(new EffectiveWorkingHours(ds.getStartTime(), ds.getEndTime()));
        }

        return Optional.empty();
    }

    public void validateDoctorWorkingAndAvailable(UUID doctorId, Instant startTime, Instant endTime) {
        ZonedDateTime startZoned = startTime.atZone(CLINIC_ZONE);
        ZonedDateTime endZoned = endTime.atZone(CLINIC_ZONE);

        LocalDate startDate = startZoned.toLocalDate();
        LocalDate endDate = endZoned.toLocalDate();
        LocalTime slotStartTime = startZoned.toLocalTime();
        LocalTime slotEndTime = endZoned.toLocalTime();

        if (!startDate.equals(endDate)) {
            throw new DoctorNotWorkingException("Khung giờ đặt lịch nằm ngoài giờ làm việc của bác sĩ.");
        }

        Optional<EffectiveWorkingHours> workingHoursOpt = resolveWorkingHours(doctorId, startDate);
        if (workingHoursOpt.isEmpty()) {
            throw new DoctorNotWorkingException("Bác sĩ không làm việc vào ngày " + startDate + ".");
        }

        EffectiveWorkingHours workingHours = workingHoursOpt.get();
        if (slotStartTime.isBefore(workingHours.startTime()) || slotEndTime.isAfter(workingHours.endTime())) {
            throw new DoctorNotWorkingException("Khung giờ đặt lịch nằm ngoài giờ làm việc của bác sĩ.");
        }

        // QTN-30 / TC-02: Check active time-off
        if (doctorTimeOffRepository.existsActiveOverlapping(doctorId, startTime, endTime)) {
            throw new DoctorNotWorkingException("Bác sĩ không làm việc trong khung giờ này.");
        }
    }
}

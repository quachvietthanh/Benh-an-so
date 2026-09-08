package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.exception.DoctorNotWorkingException;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves doctor availability against specific-date schedules, recurring weekly schedules,
 * and registered time-off intervals (QTN-30 / NCL-03-CN-006).
 */
@Service
@RequiredArgsConstructor
public class DoctorScheduleResolutionService {

    public static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorWeeklyScheduleRepository doctorWeeklyScheduleRepository;
    private final DoctorTimeOffRepository doctorTimeOffRepository;

    /**
     * Validates that the doctor is working during [startInstant, endInstant] and is not on leave.
     * Throws DoctorNotWorkingException if violated (QTN-30).
     */
    public void validateDoctorWorkingAndAvailable(UUID doctorId, Instant startInstant, Instant endInstant) {
        ZonedDateTime startZoned = startInstant.atZone(CLINIC_ZONE);
        ZonedDateTime endZoned = endInstant.atZone(CLINIC_ZONE);
        LocalDate scheduleDate = startZoned.toLocalDate();
        LocalTime startTime = startZoned.toLocalTime();
        LocalTime endTime = endZoned.toLocalTime();

        // 1. Working schedule check (date-specific schedule takes precedence over weekly recurring)
        boolean withinWorkingHours = isWithinWorkingHours(doctorId, scheduleDate, startTime, endTime);
        if (!withinWorkingHours) {
            throw new DoctorNotWorkingException("Bác sĩ không làm việc trong khung giờ này.");
        }

        // 2. Doctor time-off / leave check
        boolean onLeave = hasActiveTimeOff(doctorId, startInstant, endInstant);
        if (onLeave) {
            throw new DoctorNotWorkingException("Bác sĩ không làm việc trong khung giờ này.");
        }
    }

    public boolean isWithinWorkingHours(UUID doctorId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Optional<DoctorSchedule> dateSchedule = doctorScheduleRepository.findByDoctorIdAndScheduleDate(doctorId, date);
        if (dateSchedule.isPresent()) {
            DoctorSchedule s = dateSchedule.get();
            if (!s.isActive()) {
                return false;
            }
            return !startTime.isBefore(s.getStartTime()) && !endTime.isAfter(s.getEndTime());
        }

        // Fallback to recurring weekly schedule for the day of week
        List<DoctorWeeklySchedule> weeklySchedules = doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(
                doctorId, date.getDayOfWeek()
        );
        return weeklySchedules.stream()
                .filter(DoctorWeeklySchedule::isActive)
                .anyMatch(ws -> !startTime.isBefore(ws.getStartTime()) && !endTime.isAfter(ws.getEndTime()));
    }

    public boolean hasActiveTimeOff(UUID doctorId, Instant startInstant, Instant endInstant) {
        return !doctorTimeOffRepository.findOverlappingActiveTimeOffs(doctorId, startInstant, endInstant).isEmpty();
    }

    public Optional<DoctorScheduleWindow> resolveWorkingWindow(UUID doctorId, LocalDate date) {
        Optional<DoctorSchedule> dateSchedule = doctorScheduleRepository.findByDoctorIdAndScheduleDate(doctorId, date);
        if (dateSchedule.isPresent() && dateSchedule.get().isActive()) {
            DoctorSchedule s = dateSchedule.get();
            return Optional.of(new DoctorScheduleWindow(s.getStartTime(), s.getEndTime()));
        }

        Optional<DoctorWeeklySchedule> weeklySchedule = doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeek(
                doctorId, date.getDayOfWeek()
        );
        if (weeklySchedule.isPresent() && weeklySchedule.get().isActive()) {
            DoctorWeeklySchedule ws = weeklySchedule.get();
            return Optional.of(new DoctorScheduleWindow(ws.getStartTime(), ws.getEndTime()));
        }

        return Optional.empty();
    }

    public record DoctorScheduleWindow(LocalTime startTime, LocalTime endTime) {}
}

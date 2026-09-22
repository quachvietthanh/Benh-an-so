package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.AppointmentSeriesConflictDetail;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentSeriesValidator {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorScheduleValidator doctorScheduleValidator;
    private final DoctorTimeOffRepository doctorTimeOffRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClockPort clockPort;

    public record SessionSlot(
            int sequenceNumber,
            Instant startTime,
            Instant endTime
    ) {}

    public List<AppointmentSeriesConflictDetail> validateSessions(UUID doctorId, List<SessionSlot> sessions) {
        List<AppointmentSeriesConflictDetail> conflicts = new ArrayList<>();
        Instant now = clockPort.now();

        // 1. Check internal overlap between sessions
        for (int i = 0; i < sessions.size(); i++) {
            SessionSlot s1 = sessions.get(i);
            for (int j = i + 1; j < sessions.size(); j++) {
                SessionSlot s2 = sessions.get(j);
                if (s1.startTime().isBefore(s2.endTime()) && s1.endTime().isAfter(s2.startTime())) {
                    conflicts.add(new AppointmentSeriesConflictDetail(
                            s2.sequenceNumber(),
                            s2.startTime(),
                            s2.endTime(),
                            "INTERNAL_CONFLICT",
                            "Buổi số " + s2.sequenceNumber() + " bị trùng giờ với buổi số " + s1.sequenceNumber() + " trong cùng liệu trình."
                    ));
                }
            }
        }

        // 2. Check each session individually
        for (SessionSlot slot : sessions) {
            if (!slot.endTime().isAfter(slot.startTime())) {
                conflicts.add(new AppointmentSeriesConflictDetail(
                        slot.sequenceNumber(),
                        slot.startTime(),
                        slot.endTime(),
                        "INVALID_TIME",
                        "Thời gian kết thúc phải sau thời gian bắt đầu."
                ));
                continue;
            }

            if (slot.startTime().isBefore(now)) {
                conflicts.add(new AppointmentSeriesConflictDetail(
                        slot.sequenceNumber(),
                        slot.startTime(),
                        slot.endTime(),
                        "PAST_TIME",
                        "Thời gian đặt lịch không được ở trong quá khứ."
                ));
                continue;
            }

            ZonedDateTime startZoned = slot.startTime().atZone(CLINIC_ZONE);
            ZonedDateTime endZoned = slot.endTime().atZone(CLINIC_ZONE);
            LocalDate startDate = startZoned.toLocalDate();
            LocalDate endDate = endZoned.toLocalDate();
            LocalTime slotStartTime = startZoned.toLocalTime();
            LocalTime slotEndTime = endZoned.toLocalTime();

            if (!startDate.equals(endDate)) {
                conflicts.add(new AppointmentSeriesConflictDetail(
                        slot.sequenceNumber(),
                        slot.startTime(),
                        slot.endTime(),
                        "DOCTOR_NOT_WORKING",
                        "Khung giờ khám không được kéo dài qua ngày khác."
                ));
                continue;
            }

            // QTN-30: Check doctor working hours
            Optional<DoctorScheduleValidator.EffectiveWorkingHours> workingHoursOpt =
                    doctorScheduleValidator.resolveWorkingHours(doctorId, startDate);
            if (workingHoursOpt.isEmpty()) {
                conflicts.add(new AppointmentSeriesConflictDetail(
                        slot.sequenceNumber(),
                        slot.startTime(),
                        slot.endTime(),
                        "DOCTOR_NOT_WORKING",
                        "Bác sĩ không có ca làm việc vào ngày " + startDate + "."
                ));
                continue;
            }

            DoctorScheduleValidator.EffectiveWorkingHours workingHours = workingHoursOpt.get();
            if (slotStartTime.isBefore(workingHours.startTime()) || slotEndTime.isAfter(workingHours.endTime())) {
                conflicts.add(new AppointmentSeriesConflictDetail(
                        slot.sequenceNumber(),
                        slot.startTime(),
                        slot.endTime(),
                        "DOCTOR_NOT_WORKING",
                        "Khung giờ đặt lịch nằm ngoài ca làm việc (" + workingHours.startTime() + " - " + workingHours.endTime() + ") của bác sĩ."
                ));
                continue;
            }

            // QTN-30: Check doctor time-off
            if (doctorTimeOffRepository.existsActiveOverlapping(doctorId, slot.startTime(), slot.endTime())) {
                conflicts.add(new AppointmentSeriesConflictDetail(
                        slot.sequenceNumber(),
                        slot.startTime(),
                        slot.endTime(),
                        "DOCTOR_TIME_OFF",
                        "Bác sĩ có lịch nghỉ phép đã đăng ký trong khung giờ này."
                ));
                continue;
            }

            // QTN-04: Check appointment conflict
            if (appointmentRepository.existsActiveAppointmentConflict(doctorId, slot.startTime(), slot.endTime())) {
                conflicts.add(new AppointmentSeriesConflictDetail(
                        slot.sequenceNumber(),
                        slot.startTime(),
                        slot.endTime(),
                        "APPOINTMENT_CONFLICT",
                        "Bác sĩ đã có lịch hẹn khác trong khung giờ này."
                ));
            }
        }

        return conflicts;
    }
}

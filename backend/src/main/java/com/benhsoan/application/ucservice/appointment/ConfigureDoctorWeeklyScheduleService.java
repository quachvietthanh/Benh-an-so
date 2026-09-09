package com.benhsoan.application.ucservice.appointment;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.InvalidDoctorRoleException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.ConfigureDoctorWeeklyScheduleCommand;
import com.benhsoan.port.dto.command.appointment.WeeklyScheduleItem;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;
import com.benhsoan.port.inbound.appointment.ConfigureDoctorWeeklyScheduleUseCase;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfigureDoctorWeeklyScheduleService implements ConfigureDoctorWeeklyScheduleUseCase {

    private final DoctorWeeklyScheduleRepository weeklyScheduleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public List<DoctorWeeklyScheduleResult> configureWeeklySchedule(ConfigureDoctorWeeklyScheduleCommand command) {
        Guard.require(command, "Command");
        Guard.require(command.doctorId(), "Doctor id");
        Guard.require(command.schedules(), "Schedules list");

        // Pessimistic lock on doctor user row to serialize with concurrent appointment booking and time-off
        User doctor = userRepository.findByIdForUpdate(command.doctorId())
                .orElseThrow(() -> new DoctorNotFoundException(command.doctorId()));
        if (!doctor.isActive()) {
            throw new DoctorInactiveException(doctor.getId());
        }
        requireDoctorRole(doctor);

        ClinicConfiguration clinicConfig = clinicConfigurationRepository.find().orElse(null);

        Set<DayOfWeek> seenDays = new HashSet<>();
        List<DoctorWeeklySchedule> existingSchedules = weeklyScheduleRepository.findByDoctorId(command.doctorId());
        Map<DayOfWeek, DoctorWeeklySchedule> existingMap = new LinkedHashMap<>();
        for (DoctorWeeklySchedule ws : existingSchedules) {
            existingMap.put(ws.getDayOfWeek(), ws);
        }

        for (WeeklyScheduleItem item : command.schedules()) {
            Guard.require(item.dayOfWeek(), "Day of week");
            Guard.require(item.startTime(), "Start time");
            Guard.require(item.endTime(), "End time");

            if (!seenDays.add(item.dayOfWeek())) {
                throw new ValidationException("Ngày trong tuần không được trùng lặp: " + item.dayOfWeek());
            }

            if (!item.endTime().isAfter(item.startTime())) {
                throw new ValidationException("Giờ kết thúc phải sau giờ bắt đầu.");
            }

            if (clinicConfig != null) {
                if (item.startTime().isBefore(clinicConfig.getOpeningTime())
                        || item.endTime().isAfter(clinicConfig.getClosingTime())) {
                    throw new ValidationException("Khung giờ làm việc của bác sĩ phải nằm trong giờ mở cửa của phòng khám ("
                            + clinicConfig.getOpeningTime() + " - " + clinicConfig.getClosingTime() + ").");
                }
            }
        }

        Instant now = clockPort.now();
        List<DoctorWeeklySchedule> toSave = new ArrayList<>();

        for (WeeklyScheduleItem item : command.schedules()) {
            DoctorWeeklySchedule schedule = existingMap.get(item.dayOfWeek());
            if (schedule != null) {
                schedule.update(item.startTime(), item.endTime(), item.active(), now);
                toSave.add(schedule);
            } else {
                DoctorWeeklySchedule newSchedule = DoctorWeeklySchedule.create(
                        command.doctorId(),
                        item.dayOfWeek(),
                        item.startTime(),
                        item.endTime(),
                        now
                );
                if (!item.active()) {
                    newSchedule.update(item.startTime(), item.endTime(), false, now);
                }
                toSave.add(newSchedule);
            }
        }

        // Deactivate any existing schedules for days of week omitted from the payload (PUT collection replacement semantics)
        for (Map.Entry<DayOfWeek, DoctorWeeklySchedule> entry : existingMap.entrySet()) {
            if (!seenDays.contains(entry.getKey())) {
                DoctorWeeklySchedule existing = entry.getValue();
                if (existing.isActive()) {
                    existing.update(existing.getStartTime(), existing.getEndTime(), false, now);
                    toSave.add(existing);
                }
            }
        }

        List<DoctorWeeklySchedule> saved = weeklyScheduleRepository.saveAll(toSave);

        // Audit log (QTN-31)
        UUID currentUserId = currentUserPort.getCurrentUserId();
        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.UPDATE,
                ResourceType.DOCTOR_SCHEDULE,
                command.doctorId(),
                buildAuditDetail(command.doctorId(), saved, now),
                null,
                now
        ));

        return saved.stream()
                .map(this::toResult)
                .toList();
    }

    private void requireDoctorRole(User doctor) {
        Role doctorRole = roleRepository.findByName("DOCTOR")
                .orElseThrow(() -> new IllegalStateException("DOCTOR role is not configured."));
        if (!doctorRole.getId().equals(doctor.getRoleId())) {
            throw new InvalidDoctorRoleException(doctor.getId());
        }
    }

    private String buildAuditDetail(UUID doctorId, List<DoctorWeeklySchedule> schedules, Instant now) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("doctorId", doctorId.toString());
        detail.put("updatedAt", now.toString());
        detail.put("scheduleCount", schedules.size());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{\"doctorId\":\"" + doctorId + "\"}";
        }
    }

    private DoctorWeeklyScheduleResult toResult(DoctorWeeklySchedule ws) {
        return new DoctorWeeklyScheduleResult(
                ws.getId(),
                ws.getDoctorId(),
                ws.getDayOfWeek(),
                ws.getStartTime(),
                ws.getEndTime(),
                ws.isActive()
        );
    }
}

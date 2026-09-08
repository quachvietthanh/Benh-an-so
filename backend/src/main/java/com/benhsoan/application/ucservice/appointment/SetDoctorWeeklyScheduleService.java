package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.DoctorWeeklyScheduleItemCommand;
import com.benhsoan.port.dto.command.appointment.SetDoctorWeeklyScheduleCommand;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleItemResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;
import com.benhsoan.port.inbound.appointment.SetDoctorWeeklyScheduleUseCase;
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

/**
 * NCL-03-CN-006 CV-03 / TC-01: sets a doctor's recurring weekly working schedule,
 * validating against doctor status, clinic operating hours and recording an audit trail.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SetDoctorWeeklyScheduleService implements SetDoctorWeeklyScheduleUseCase {

    private final DoctorWeeklyScheduleRepository doctorWeeklyScheduleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public DoctorWeeklyScheduleResult setWeeklySchedule(SetDoctorWeeklyScheduleCommand command) {
        if (command == null || command.doctorId() == null) {
            throw new ValidationException("doctorId is required.");
        }

        User doctor = userRepository.findByIdForUpdate(command.doctorId())
                .orElseThrow(() -> new DoctorNotFoundException(command.doctorId()));
        if (!doctor.isActive()) {
            throw new DoctorInactiveException(doctor.getId());
        }
        requireDoctorRole(doctor);

        List<DoctorWeeklyScheduleItemCommand> items = command.items() != null ? command.items() : List.of();

        // Validate clinic hours constraint (NCL-09-CN-002 precondition)
        clinicConfigurationRepository.find().ifPresent(clinic -> validateWithinClinicHours(items, clinic));

        // Validate no overlapping active slots on the same weekday
        validateNoOverlaps(items);

        // Replace existing weekly schedules
        doctorWeeklyScheduleRepository.deleteByDoctorId(command.doctorId());

        List<DoctorWeeklySchedule> domainList = items.stream()
                .map(item -> DoctorWeeklySchedule.create(
                        command.doctorId(),
                        item.dayOfWeek(),
                        item.startTime(),
                        item.endTime(),
                        item.active()
                ))
                .toList();

        List<DoctorWeeklySchedule> savedList = doctorWeeklyScheduleRepository.saveAll(domainList);

        Instant now = clockPort.now();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.UPDATE,
                ResourceType.DOCTOR_SCHEDULE,
                doctor.getId(),
                auditDetail(command.doctorId(), savedList, now),
                null,
                now
        ));

        List<DoctorWeeklyScheduleItemResult> itemResults = savedList.stream()
                .sorted(Comparator.comparing(DoctorWeeklySchedule::getDayOfWeek)
                        .thenComparing(DoctorWeeklySchedule::getStartTime))
                .map(s -> new DoctorWeeklyScheduleItemResult(
                        s.getId(),
                        s.getDoctorId(),
                        s.getDayOfWeek(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.isActive()
                ))
                .toList();

        return new DoctorWeeklyScheduleResult(command.doctorId(), itemResults);
    }

    private void requireDoctorRole(User doctor) {
        Role doctorRole = roleRepository.findByName("DOCTOR")
                .orElseThrow(() -> new IllegalStateException("DOCTOR role is not configured."));
        if (!doctorRole.getId().equals(doctor.getRoleId())) {
            throw new InvalidDoctorRoleException(doctor.getId());
        }
    }

    private void validateWithinClinicHours(List<DoctorWeeklyScheduleItemCommand> items, ClinicConfiguration clinic) {
        for (DoctorWeeklyScheduleItemCommand item : items) {
            if (item.startTime().isBefore(clinic.getOpeningTime()) || item.endTime().isAfter(clinic.getClosingTime())) {
                throw new ValidationException(
                        "Doctor schedule for " + item.dayOfWeek() + " (" + item.startTime() + " - " + item.endTime()
                                + ") exceeds clinic operating hours (" + clinic.getOpeningTime() + " - " + clinic.getClosingTime() + ")."
                );
            }
        }
    }

    private void validateNoOverlaps(List<DoctorWeeklyScheduleItemCommand> items) {
        for (int i = 0; i < items.size(); i++) {
            DoctorWeeklyScheduleItemCommand first = items.get(i);
            for (int j = i + 1; j < items.size(); j++) {
                DoctorWeeklyScheduleItemCommand second = items.get(j);
                if (first.dayOfWeek() == second.dayOfWeek()) {
                    boolean overlap = first.startTime().isBefore(second.endTime())
                            && first.endTime().isAfter(second.startTime());
                    if (overlap) {
                        throw new ValidationException("Overlapping schedule intervals for day: " + first.dayOfWeek());
                    }
                }
            }
        }
    }

    private String auditDetail(UUID doctorId, List<DoctorWeeklySchedule> schedules, Instant now) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("doctorId", doctorId.toString());
        detail.put("totalDaysConfigured", schedules.size());
        detail.put("updatedAt", now.toString());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize weekly schedule audit detail.", e);
        }
    }
}

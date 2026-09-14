package com.benhsoan.application.ucservice.appointment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.AppointmentRescheduleLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.appointment.AppointmentRescheduleHistoryResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRescheduleLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentRescheduleHistoryAssembler {

    private final AppointmentRescheduleLogRepository rescheduleLogRepository;
    private final UserRepository userRepository;

    public List<AppointmentRescheduleHistoryResult> getHistoriesForAppointment(UUID appointmentId) {
        List<AppointmentRescheduleLog> logs = rescheduleLogRepository.findByAppointmentId(appointmentId);
        return toResults(logs);
    }

    public List<AppointmentRescheduleHistoryResult> toResults(List<AppointmentRescheduleLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }

        Set<UUID> userIds = logs.stream()
                .flatMap(log -> Stream.of(log.getOldDoctorId(), log.getNewDoctorId(), log.getRescheduledBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> userNames = userIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(new ArrayList<>(userIds)).stream()
                        .collect(Collectors.toMap(User::getId, User::getFullName, (existing, replace) -> existing));

        return logs.stream()
                .map(log -> toResult(log, userNames))
                .toList();
    }

    public AppointmentRescheduleHistoryResult toResult(AppointmentRescheduleLog log) {
        if (log == null) {
            return null;
        }
        return toResults(List.of(log)).getFirst();
    }

    private AppointmentRescheduleHistoryResult toResult(AppointmentRescheduleLog log, Map<UUID, String> userNames) {
        String oldDocName = userNames.getOrDefault(log.getOldDoctorId(), "Unknown");
        String newDocName = userNames.getOrDefault(log.getNewDoctorId(), "Unknown");
        String performerName = userNames.getOrDefault(log.getRescheduledBy(), "Unknown");

        return AppointmentRescheduleHistoryResult.builder()
                .id(log.getId())
                .appointmentId(log.getAppointmentId())
                .oldDoctorId(log.getOldDoctorId())
                .newDoctorId(log.getNewDoctorId())
                .oldDoctorName(oldDocName)
                .newDoctorName(newDocName)
                .oldStartTime(log.getOldStartTime())
                .oldEndTime(log.getOldEndTime())
                .newStartTime(log.getNewStartTime())
                .newEndTime(log.getNewEndTime())
                .reason(log.getReason())
                .rescheduledBy(log.getRescheduledBy())
                .rescheduledByName(performerName)
                .rescheduledAt(log.getRescheduledAt())
                .build();
    }
}

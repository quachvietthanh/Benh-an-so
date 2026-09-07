package com.benhsoan.application.ucservice.security;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.config.AnomalyDetectionProperties;
import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertSeverity;
import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.port.inbound.security.ScanAccessLogAnomaliesUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScanAccessLogAnomaliesService implements ScanAccessLogAnomaliesUseCase {

    private final MedicalRecordAccessLogRepository accessLogRepository;
    private final SecurityAlertRepository securityAlertRepository;
    private final AnomalyDetectionProperties properties;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public void scan() {
        Instant now = clockPort.now();
        Instant windowStart = now.truncatedTo(ChronoUnit.HOURS);
        Instant windowEnd = windowStart.plus(1, ChronoUnit.HOURS);

        List<MedicalRecordAccessLog> views = accessLogRepository.findViewsBetween(windowStart, windowEnd);
        if (views.isEmpty()) {
            return;
        }

        Map<UUID, List<MedicalRecordAccessLog>> viewsByUser = views.stream()
                .collect(Collectors.groupingBy(MedicalRecordAccessLog::getAccessedBy));

        viewsByUser.forEach((userId, userViews) -> {
            detectThresholdExceeded(userId, userViews, windowStart, windowEnd, now);
            detectOffHoursAccess(userId, userViews, windowStart, windowEnd, now);
        });
    }

    private void detectThresholdExceeded(UUID userId, List<MedicalRecordAccessLog> views,
            Instant windowStart, Instant windowEnd, Instant now) {
        int count = views.size();
        if (count <= properties.maxViewsPerHour()) {
            return;
        }
        saveIfAbsent(SecurityAlert.create(
                userId,
                AlertType.THRESHOLD_EXCEEDED,
                AlertSeverity.HIGH,
                "User opened " + count + " medical records within one hour (threshold "
                        + properties.maxViewsPerHour() + ").",
                count,
                windowStart,
                windowEnd,
                now));
    }

    private void detectOffHoursAccess(UUID userId, List<MedicalRecordAccessLog> views,
            Instant windowStart, Instant windowEnd, Instant now) {
        List<MedicalRecordAccessLog> offHoursViews = views.stream()
                .filter(view -> isOffHours(view.getAccessedAt()))
                .toList();
        if (offHoursViews.isEmpty()) {
            return;
        }
        saveIfAbsent(SecurityAlert.create(
                userId,
                AlertType.OFF_HOURS_ACCESS,
                AlertSeverity.LOW,
                "User accessed medical records outside working hours ("
                        + offHoursViews.size() + " access(es)).",
                offHoursViews.size(),
                windowStart,
                windowEnd,
                now));
    }

    private boolean isOffHours(Instant accessedAt) {
        LocalTime time = accessedAt.atZone(properties.zoneId()).toLocalTime();
        return time.isBefore(properties.workingHoursStart())
                || !time.isBefore(properties.workingHoursEnd());
    }

    private void saveIfAbsent(SecurityAlert candidate) {
        if (securityAlertRepository.existsByUserIdAndAlertTypeAndWindowStart(
                candidate.getUserId(), candidate.getAlertType(), candidate.getWindowStart())) {
            return;
        }
        securityAlertRepository.save(candidate);
    }
}

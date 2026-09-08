package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.appointment.exception.DoctorTimeOffNotFoundException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.inbound.appointment.CancelDoctorTimeOffUseCase;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelDoctorTimeOffService implements CancelDoctorTimeOffUseCase {

    private final DoctorTimeOffRepository doctorTimeOffRepository;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public DoctorTimeOffResult cancelTimeOff(UUID id) {
        DoctorTimeOff timeOff = doctorTimeOffRepository.findById(id)
                .orElseThrow(() -> new DoctorTimeOffNotFoundException(id));

        if (timeOff.getStatus() == TimeOffStatus.CANCELLED) {
            return toResult(timeOff);
        }

        Instant now = clockPort.now();
        timeOff.cancel(now);

        DoctorTimeOff saved = doctorTimeOffRepository.save(timeOff);

        auditLogRepository.save(AuditLog.create(
                currentUserPort.getCurrentUserId(),
                ActionType.DELETE,
                ResourceType.DOCTOR_TIME_OFF,
                saved.getId(),
                auditDetail(saved, now),
                null,
                now
        ));

        return toResult(saved);
    }

    private DoctorTimeOffResult toResult(DoctorTimeOff saved) {
        return new DoctorTimeOffResult(
                saved.getId(),
                saved.getDoctorId(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getReason(),
                saved.getStatus(),
                saved.getCreatedBy(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                List.of()
        );
    }

    private String auditDetail(DoctorTimeOff timeOff, Instant now) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("timeOffId", timeOff.getId().toString());
        detail.put("doctorId", timeOff.getDoctorId().toString());
        detail.put("status", timeOff.getStatus().name());
        detail.put("cancelledAt", now.toString());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize cancel doctor time-off audit detail.", e);
        }
    }
}

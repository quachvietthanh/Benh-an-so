package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.DoctorTimeOffNotFoundException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.inbound.appointment.CancelDoctorTimeOffUseCase;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
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
    private final UserRepository userRepository;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public DoctorTimeOffResult cancelTimeOff(UUID doctorId, UUID timeOffId) {
        userRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));

        DoctorTimeOff timeOff = doctorTimeOffRepository.findById(timeOffId)
                .orElseThrow(() -> new DoctorTimeOffNotFoundException(timeOffId));

        if (!timeOff.getDoctorId().equals(doctorId)) {
            throw new ValidationException("Khoảng nghỉ không thuộc về bác sĩ này.");
        }

        Instant now = clockPort.now();
        timeOff.cancel(now);
        DoctorTimeOff saved = doctorTimeOffRepository.save(timeOff);

        auditLogRepository.save(AuditLog.create(
                currentUserPort.getCurrentUserId(),
                ActionType.CANCEL,
                ResourceType.DOCTOR_TIMEOFF,
                saved.getId(),
                buildAuditDetail(now),
                null,
                now
        ));

        return new DoctorTimeOffResult(
                saved.getId(),
                saved.getDoctorId(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getReason(),
                saved.getStatus(),
                saved.getCreatedBy(),
                saved.getCreatedAt(),
                List.of()
        );
    }

    private String buildAuditDetail(Instant now) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("status", "CANCELLED");
        detail.put("cancelledAt", now.toString());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{\"status\":\"CANCELLED\"}";
        }
    }
}

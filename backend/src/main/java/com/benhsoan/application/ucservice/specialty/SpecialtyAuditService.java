package com.benhsoan.application.ucservice.specialty;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpecialtyAuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ObjectMapper objectMapper;

    public void record(ActionType actionType, Specialty specialty, Collection<UUID> doctorIds, Collection<UUID> roomIds) {
        auditLogRepository.save(AuditLog.create(
                currentUserPort.getCurrentUserId(),
                actionType,
                ResourceType.SPECIALTY,
                specialty.getId(),
                serializeDetail(specialty, doctorIds, roomIds),
                null
        ));
    }

    public void record(ActionType actionType, Specialty specialty) {
        record(actionType, specialty, null, null);
    }

    private String serializeDetail(Specialty specialty, Collection<UUID> doctorIds, Collection<UUID> roomIds) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("specialtyId", specialty.getId());
        detail.put("code", specialty.getCode());
        detail.put("name", specialty.getName());
        detail.put("description", specialty.getDescription());
        detail.put("status", specialty.isActive() ? "ACTIVE" : "INACTIVE");
        if (doctorIds != null) {
            detail.put("doctorIds", doctorIds);
        }
        if (roomIds != null) {
            detail.put("roomIds", roomIds);
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize specialty audit detail.", exception);
        }
    }
}

package com.benhsoan.application.ucservice.queue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.queue.Room;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class RoomAuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ObjectMapper objectMapper;

    void record(ActionType actionType, Room room) {
        auditLogRepository.save(AuditLog.create(
                currentUserPort.getCurrentUserId(),
                actionType,
                ResourceType.ROOM,
                room.getId(),
                serializeDetail(room),
                null
        ));
    }

    private String serializeDetail(Room room) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("roomId", room.getId());
        detail.put("code", room.getCode());
        detail.put("name", room.getName());
        detail.put("status", room.isActive() ? "ACTIVE" : "INACTIVE");
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize room audit detail.", exception);
        }
    }
}

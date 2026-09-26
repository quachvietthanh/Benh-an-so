package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.queue.DoctorRoomAssignment;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor
class DoctorRoomAssignmentAuditService {
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    void record(ActionType action, DoctorRoomAssignment assignment) {
        String detail = "{\"doctorId\":\"%s\",\"roomId\":\"%s\"}".formatted(assignment.getDoctorId(), assignment.getRoomId());
        auditLogRepository.save(AuditLog.create(currentUserPort.getCurrentUserId(), action, ResourceType.ROOM, assignment.getRoomId(), detail, null));
    }
}

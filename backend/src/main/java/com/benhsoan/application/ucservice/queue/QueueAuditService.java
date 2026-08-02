package com.benhsoan.application.ucservice.queue;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.port.outbound.repository.logRepository.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class QueueAuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;

    void record(ActionType actionType, QueueItem item) {
        UUID actorId = currentUserPort.getCurrentUserId();
        auditLogRepository.save(AuditLog.create(actorId, actionType, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"status\":\"%s\"}".formatted(item.getId(), item.getStatus()), null));
    }

    void recordSkipped(QueueItem item, String reasonCode) {
        UUID actorId = currentUserPort.getCurrentUserId();
        auditLogRepository.save(AuditLog.create(actorId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"reason\":\"%s\"}"
                        .formatted(item.getId(), item.getStatus(), reasonCode),
                null));
    }
}

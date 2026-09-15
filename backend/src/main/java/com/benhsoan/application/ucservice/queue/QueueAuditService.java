package com.benhsoan.application.ucservice.queue;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.enums.QueueSemanticAction;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class QueueAuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;

    void recordCheckIn(QueueItem item, QueueItemSourceType sourceType, int queueNumber, UUID actorId) {
        UUID effectiveActorId = actorId != null ? actorId : currentUserPort.getCurrentUserId();
        auditLogRepository.save(AuditLog.create(effectiveActorId, ActionType.CREATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"sourceType\":\"%s\",\"queueNumber\":%d,\"status\":\"%s\",\"action\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), sourceType, queueNumber, QueueItemStatus.WAITING, QueueSemanticAction.CHECK_IN, 0),
                null));
    }

    void recordCall(QueueItem item) {
        UUID actorId = currentUserPort.getCurrentUserId();
        auditLogRepository.save(AuditLog.create(actorId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"action\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), item.getStatus(), QueueSemanticAction.CALL, item.getCallCount()),
                null));
    }

    void recordCompleted(QueueItem item) {
        UUID actorId = currentUserPort.getCurrentUserId();
        auditLogRepository.save(AuditLog.create(actorId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"action\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), item.getStatus(), QueueSemanticAction.COMPLETED, item.getCallCount()),
                null));
    }

    void recordCancelled(QueueItem item, String reason) {
        UUID actorId = currentUserPort.getCurrentUserId();
        String detail = reason != null
                ? "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"action\":\"%s\",\"reason\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), item.getStatus(), QueueSemanticAction.CANCELLED, reason, item.getCallCount())
                : "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"action\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), item.getStatus(), QueueSemanticAction.CANCELLED, item.getCallCount());
        auditLogRepository.save(AuditLog.create(actorId, ActionType.CANCEL, ResourceType.VISIT, item.getVisitId(),
                detail, null));
    }

    void recordEarlyEnded(QueueItem item, String reason) {
        UUID actorId = currentUserPort.getCurrentUserId();
        String detail = reason != null
                ? "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"action\":\"%s\",\"reason\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), item.getStatus(), QueueSemanticAction.EARLY_ENDED, reason, item.getCallCount())
                : "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"action\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), item.getStatus(), QueueSemanticAction.EARLY_ENDED, item.getCallCount());
        auditLogRepository.save(AuditLog.create(actorId, ActionType.CANCEL, ResourceType.VISIT, item.getVisitId(),
                detail, null));
    }

    void recordSkipped(QueueItem item, String reasonCode) {
        UUID actorId = currentUserPort.getCurrentUserId();
        auditLogRepository.save(AuditLog.create(actorId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"action\":\"%s\",\"reason\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), item.getStatus(), QueueSemanticAction.DEFERRED, reasonCode, item.getCallCount()),
                null));
    }

    void recordReQueued(QueueItem item) {
        UUID actorId = currentUserPort.getCurrentUserId();
        auditLogRepository.save(AuditLog.create(actorId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"action\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), item.getStatus(), QueueSemanticAction.RE_QUEUED, item.getCallCount()),
                null));
    }

    void record(ActionType actionType, QueueItem item) {
        UUID actorId = currentUserPort.getCurrentUserId();
        String actionName = switch (item.getStatus()) {
            case IN_PROGRESS -> QueueSemanticAction.CALL.name();
            case COMPLETED -> QueueSemanticAction.COMPLETED.name();
            case CANCELLED -> QueueSemanticAction.CANCELLED.name();
            case SKIPPED -> QueueSemanticAction.DEFERRED.name();
            case WAITING -> QueueSemanticAction.CHECK_IN.name();
            default -> actionType.name();
        };
        auditLogRepository.save(AuditLog.create(actorId, actionType, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"status\":\"%s\",\"action\":\"%s\",\"callCount\":%d}"
                        .formatted(item.getId(), item.getStatus(), actionName, item.getCallCount()), null));
    }
}


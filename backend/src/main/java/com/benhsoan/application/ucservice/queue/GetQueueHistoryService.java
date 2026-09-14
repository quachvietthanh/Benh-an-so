package com.benhsoan.application.ucservice.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.port.dto.result.QueueHistoryResult;
import com.benhsoan.port.inbound.queue.GetQueueHistoryUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetQueueHistoryService implements GetQueueHistoryUseCase {

    private final QueueItemRepository queueItemRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final QueueOperationAuthorization authorization;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<QueueHistoryResult> getHistory(UUID queueItemId) {
        QueueItem item = queueItemRepository.findByIdForUpdate(queueItemId)
                .orElseThrow(() -> new QueueItemNotFoundException(queueItemId));
        var queue = medicalQueueRepository.findById(item.getMedicalQueueId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getMedicalQueueId()));
        authorization.requireReadPermission(queue);

        List<AuditLog> logs = auditLogRepository.findByResourceTypeAndResourceId(ResourceType.VISIT, item.getVisitId());
        List<QueueHistoryResult> results = new ArrayList<>();

        for (AuditLog log : logs) {
            String detail = log.getDetail();
            String status = null;
            String reason = null;
            String action = log.getActionType().name();
            int callCount = item.getCallCount();

            if (detail != null && !detail.isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(detail);
                    if (node.has("status")) {
                        status = node.get("status").asText();
                    }
                    if (node.has("reason")) {
                        reason = node.get("reason").asText();
                    }
                    if (node.has("action")) {
                        action = node.get("action").asText();
                    }
                    if (node.has("callCount")) {
                        callCount = node.get("callCount").asInt();
                    }
                } catch (Exception ignored) {
                }
            }

            String operatorName = userRepository.findById(log.getUserId())
                    .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername())
                    .orElse("Hệ thống");

            results.add(new QueueHistoryResult(
                    log.getId(),
                    item.getId(),
                    log.getUserId(),
                    operatorName,
                    action,
                    status != null ? status : item.getStatus().name(),
                    callCount,
                    reason != null ? reason : item.getSkipReason(),
                    log.getCreatedAt()
            ));
        }

        return results;
    }
}

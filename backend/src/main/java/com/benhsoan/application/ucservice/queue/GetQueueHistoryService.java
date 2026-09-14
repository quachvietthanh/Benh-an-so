package com.benhsoan.application.ucservice.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.domain.queue.exception.QueueNotFoundException;
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

    private record ParsedQueueLog(
            AuditLog log,
            String action,
            String status,
            int callCount,
            String reason
    ) {}

    @Override
    public List<QueueHistoryResult> getHistory(UUID queueItemId) {
        QueueItem item = queueItemRepository.findById(queueItemId)
                .orElseThrow(() -> new QueueItemNotFoundException(queueItemId));
        var queue = medicalQueueRepository.findById(item.getMedicalQueueId())
                .orElseThrow(() -> new QueueNotFoundException(item.getMedicalQueueId()));
        authorization.requireReadPermission(queue);

        List<AuditLog> logs = auditLogRepository.findByResourceTypeAndResourceId(ResourceType.VISIT, item.getVisitId());
        List<ParsedQueueLog> parsedLogs = new ArrayList<>();

        for (AuditLog log : logs) {
            String detail = log.getDetail();
            if (detail == null || detail.isBlank()) {
                continue;
            }

            try {
                JsonNode node = objectMapper.readTree(detail);
                if (!node.has("queueItemId") || !item.getId().toString().equals(node.get("queueItemId").asText())) {
                    continue;
                }

                String action = node.has("action") ? node.get("action").asText() : log.getActionType().name();
                String status = node.has("status") ? node.get("status").asText() : item.getStatus().name();
                int callCount = node.has("callCount") ? node.get("callCount").asInt() : 0;
                String reason = node.has("reason") ? node.get("reason").asText() : null;

                parsedLogs.add(new ParsedQueueLog(log, action, status, callCount, reason));
            } catch (Exception ignored) {
            }
        }

        List<UUID> userIds = parsedLogs.stream()
                .map(p -> p.log().getUserId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> operatorNameMap = userIds.isEmpty() ? Map.of() :
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(
                                User::getId,
                                u -> u.getFullName() != null ? u.getFullName() : u.getUsername(),
                                (existing, replace) -> existing
                        ));

        List<QueueHistoryResult> results = new ArrayList<>();
        for (ParsedQueueLog parsed : parsedLogs) {
            AuditLog log = parsed.log();
            String operatorName = log.getUserId() != null
                    ? operatorNameMap.getOrDefault(log.getUserId(), "Hệ thống")
                    : "Hệ thống";

            results.add(new QueueHistoryResult(
                    log.getId(),
                    item.getId(),
                    log.getUserId(),
                    operatorName,
                    parsed.action(),
                    parsed.status(),
                    parsed.callCount(),
                    parsed.reason(),
                    log.getCreatedAt()
            ));
        }

        return results;
    }
}

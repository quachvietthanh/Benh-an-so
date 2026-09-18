package com.benhsoan.application.ucservice.auditlog;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.command.auditlog.AdminOperationLogQuery;
import com.benhsoan.port.dto.result.auditlog.AdminOperationLogResult;
import com.benhsoan.port.inbound.auditlog.GetAdminOperationLogsUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAdminOperationLogsService implements GetAdminOperationLogsUseCase {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    public Page<AdminOperationLogResult> getLogs(AdminOperationLogQuery query, Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findAdminOperationLogs(
                query.actorId(), query.resourceType(), query.from(), query.to(), pageable);

        Map<UUID, User> usersById = userRepository.findAllById(
                        logs.getContent().stream()
                                .map(AuditLog::getUserId)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return logs.map(log -> toResult(log, usersById.get(log.getUserId())));
    }

    private AdminOperationLogResult toResult(AuditLog log, User actor) {
        return new AdminOperationLogResult(
                log.getId(),
                log.getUserId(),
                actor == null ? null : actor.getFullName(),
                log.getActionType(),
                log.getResourceType(),
                log.getResourceId(),
                log.getDetail(),
                log.getCreatedAt());
    }
}

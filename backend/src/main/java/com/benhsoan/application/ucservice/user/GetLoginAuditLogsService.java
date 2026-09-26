package com.benhsoan.application.ucservice.user;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.result.LoginAuditLogResult;
import com.benhsoan.port.inbound.user.GetLoginAuditLogsUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetLoginAuditLogsService implements GetLoginAuditLogsUseCase {

    private final UserRepository userRepository;

    private final AuditLogRepository auditLogRepository;

    @Override
    public Page<LoginAuditLogResult> getLoginAuditLogs(UUID userId, Pageable pageable) {

        userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return auditLogRepository.findLoginAuditLogs(userId, pageable)
                .map(log -> new LoginAuditLogResult(
                        log.getId(),
                        log.getUserId(),
                        log.getActionType(),
                        log.getResourceType(),
                        log.getResourceId(),
                        log.getDetail(),
                        log.getIpAddress(),
                        log.getCreatedAt()
                ));
    }
}

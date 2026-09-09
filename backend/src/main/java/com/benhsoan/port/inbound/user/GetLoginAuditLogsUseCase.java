package com.benhsoan.port.inbound.user;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.LoginAuditLogResult;

public interface GetLoginAuditLogsUseCase {

    Page<LoginAuditLogResult> getLoginAuditLogs(UUID userId, Pageable pageable);
}

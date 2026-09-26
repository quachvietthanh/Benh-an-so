package com.benhsoan.port.inbound.auditlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.command.auditlog.AdminOperationLogQuery;
import com.benhsoan.port.dto.result.auditlog.AdminOperationLogResult;

public interface GetAdminOperationLogsUseCase {

    Page<AdminOperationLogResult> getLogs(AdminOperationLogQuery query, Pageable pageable);
}

package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReportAuthorizer {

    private static final String PHARMACIST_ROLE = "PHARMACIST";
    private static final String MANAGER_ROLE = "MANAGER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;

    public void requireReportAccess() {
        if (!currentUserPort.hasRole(PHARMACIST_ROLE)
                && !currentUserPort.hasRole(MANAGER_ROLE)
                && !currentUserPort.hasRole(ADMIN_ROLE)) {
            String message = "Chỉ dược sĩ, quản lý phòng khám hoặc quản trị viên mới có quyền truy cập báo cáo xuất nhập tồn kho.";
            recordAccessDenied(message);
            throw new AccessDeniedException(message);
        }
    }

    private void recordAccessDenied(String reason) {
        try {
            UUID userId = currentUserPort.getCurrentUserId();
            Instant now = clockPort.now();
            Set<String> roles = currentUserPort.getCurrentUserRoles();
            String rolesStr = roles != null ? String.join(",", roles) : "NONE";
            String detailJson = """
                    {"resource":"INVENTORY_IN_OUT_STOCK_REPORT","roles":"%s","reason":"%s","deniedAt":"%s"}
                    """.formatted(rolesStr, reason, now);

            auditLogRepository.save(AuditLog.create(
                    userId,
                    ActionType.ACCESS_DENIED,
                    ResourceType.OPERATIONAL_REPORT,
                    null,
                    detailJson,
                    null,
                    now
            ));
        } catch (RuntimeException ex) {
            log.warn("Failed to record access denied audit log for inventory report: {}", ex.getMessage());
        }
    }
}

package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

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
    private final InventoryReportAccessDeniedAuditWriter auditWriter;
    private final ClockPort clockPort;

    public void requireReportAccess() {
        if (!currentUserPort.hasRole(PHARMACIST_ROLE)
                && !currentUserPort.hasRole(MANAGER_ROLE)
                && !currentUserPort.hasRole(ADMIN_ROLE)) {
            String message = "Chỉ dược sĩ, quản lý phòng khám hoặc quản trị viên mới có quyền truy cập báo cáo xuất nhập tồn kho.";
            UUID userId = currentUserPort.getCurrentUserId();
            Set<String> roles = currentUserPort.getCurrentUserRoles();
            Instant now = clockPort.now();
            auditWriter.writeAccessDenied(userId, roles, message, now);
            throw new AccessDeniedException(message);
        }
    }
}

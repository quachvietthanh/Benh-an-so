package com.benhsoan.application.ucservice.inventory;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * Kiểm soát phân quyền tại tầng Application Service Layer cho phân hệ dự trù mua thuốc (NCL-06-CN-012).
 * Bảo đảm use case tự bảo vệ độc lập, không phụ thuộc hoàn toàn vào Controller.
 */
@Component
@RequiredArgsConstructor
public class MedicationProcurementAuthorizer {

    public static final String PERMISSION_READ = "MEDICATION_PROCUREMENT_READ";
    public static final String PERMISSION_CREATE = "MEDICATION_PROCUREMENT_CREATE";
    public static final String PERMISSION_APPROVE = "MEDICATION_PROCUREMENT_APPROVE";
    public static final String ADMIN_ROLE = "ADMIN";

    private final CurrentUserPort currentUserPort;

    public void requireReadPermission() {
        if (!currentUserPort.hasPermission(PERMISSION_READ) && !currentUserPort.hasRole(ADMIN_ROLE)) {
            throw new AccessDeniedException("Bạn không có quyền xem gợi ý hoặc danh sách phiếu dự trù mua thuốc.");
        }
    }

    public void requireCreatePermission() {
        if (!currentUserPort.hasPermission(PERMISSION_CREATE) && !currentUserPort.hasRole(ADMIN_ROLE)) {
            throw new AccessDeniedException("Bạn không có quyền lập hoặc điều chỉnh phiếu dự trù mua thuốc.");
        }
    }

    public void requireApprovePermission() {
        if (!currentUserPort.hasPermission(PERMISSION_APPROVE) && !currentUserPort.hasRole(ADMIN_ROLE)) {
            throw new AccessDeniedException("Bạn không có quyền phê duyệt hoặc từ chối phiếu dự trù mua thuốc.");
        }
    }
}

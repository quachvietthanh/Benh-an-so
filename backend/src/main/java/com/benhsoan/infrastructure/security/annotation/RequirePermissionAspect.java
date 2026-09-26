package com.benhsoan.infrastructure.security.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class RequirePermissionAspect {
    private final PermissionEvaluator permissionEvaluator;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;

    @Around("@annotation(requirePermission)")
    public Object requirePermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        boolean allowed = requirePermission.operator() == RequirePermission.Operator.ALL
                ? permissionEvaluator.hasAllPermissions(requirePermission.value())
                : permissionEvaluator.hasAnyPermission(requirePermission.value());
        if (!allowed) {
            auditDeniedAttempt(requirePermission);
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này");
        }
        return joinPoint.proceed();
    }

    private void auditDeniedAttempt(RequirePermission requirement) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String path = attributes == null ? null : attributes.getRequest().getRequestURI();
            String method = attributes == null ? null : attributes.getRequest().getMethod();
            String detail = "{\"requiredPermissions\":\"%s\",\"operator\":\"%s\",\"method\":\"%s\",\"path\":\"%s\"}"
                    .formatted(String.join(",", requirement.value()), requirement.operator(), method, path);
            auditLogRepository.save(AuditLog.create(currentUserPort.getCurrentUserId(), ActionType.ACCESS_DENIED,
                    ResourceType.PERMISSION, null, detail, null));
        } catch (RuntimeException ignored) {
            // Authorization must remain denied even if the audit write cannot be completed.
        }
    }
}

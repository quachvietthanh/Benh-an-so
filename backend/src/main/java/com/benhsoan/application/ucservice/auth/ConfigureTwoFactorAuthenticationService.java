package com.benhsoan.application.ucservice.auth;

import java.util.Locale;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.auth.ConfigureTwoFactorAuthenticationCommand;
import com.benhsoan.port.dto.result.TwoFactorConfigurationResult;
import com.benhsoan.port.inbound.auth.TwoFactorAuthenticationConfigurationUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfigureTwoFactorAuthenticationService implements TwoFactorAuthenticationConfigurationUseCase {

    private static final String MANAGE_PERMISSION = "TWO_FACTOR_AUTH_MANAGE";
    private static final Set<String> SUPPORTED_ROLES = Set.of("ADMIN", "DOCTOR");

    private final RoleRepository roleRepository;
    private final CurrentUserPort currentUserPort;
    private final AdminOperationAuditService adminOperationAuditService;
    private final ClockPort clockPort;

    @Override
    public TwoFactorConfigurationResult configure(ConfigureTwoFactorAuthenticationCommand command) {
        String roleName = normalize(command.roleName());

        if (!SUPPORTED_ROLES.contains(roleName)) {
            throw new ValidationException("Chỉ hỗ trợ bật/tắt xác thực hai lớp cho vai trò ADMIN hoặc DOCTOR.");
        }

        // Authorization must be enforced at the service layer, not only in the controller.
        if (!currentUserPort.hasPermission(MANAGE_PERMISSION)) {
            throw new AccessDeniedException("Bạn không có quyền cấu hình xác thực hai lớp.");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(RoleNotFoundException::new);

        boolean previous = role.isTwoFactorRequired();
        if (previous == command.enabled()) {
            return new TwoFactorConfigurationResult(role.getName(), previous);
        }

        role.setTwoFactorRequired(command.enabled());
        roleRepository.save(role);

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.UPDATE,
                ResourceType.ROLE,
                role.getId(),
                AdminOperationAuditService.fields("twoFactorRequired", previous),
                AdminOperationAuditService.fields("twoFactorRequired", command.enabled()),
                clockPort.now()
        );

        return new TwoFactorConfigurationResult(role.getName(), command.enabled());
    }

    private String normalize(String roleName) {
        return roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
    }
}

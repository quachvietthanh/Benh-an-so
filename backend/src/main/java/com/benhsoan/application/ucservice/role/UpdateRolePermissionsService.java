package com.benhsoan.application.ucservice.role;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Permission;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.role.UpdateRolePermissionsCommand;
import com.benhsoan.port.dto.result.role.RolePermissionsResult;
import com.benhsoan.port.inbound.role.UpdateRolePermissionsUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.PermissionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateRolePermissionsService implements UpdateRolePermissionsUseCase {
    private static final Set<String> ROLE_MANAGEMENT_PERMISSIONS = Set.of("ROLE_READ", "ROLE_UPDATE", "PERMISSION_READ");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final RolePermissionsResultMapper mapper;

    @Override
    public RolePermissionsResult updateRolePermissions(UpdateRolePermissionsCommand command) {
        if (command == null || command.roleId() == null) throw new ValidationException("Role id is required.");

        List<String> codes = command.permissionCodes() == null ? List.of() : command.permissionCodes();
        if (codes.stream().anyMatch(code -> code == null || code.isBlank())) {
            throw new ValidationException("Permission code is required.");
        }
        Set<String> requestedCodes = new HashSet<>(codes);
        if (requestedCodes.size() != codes.size()) throw new ValidationException("Permission codes must not be duplicated.");

        Role role = roleRepository.findById(command.roleId()).orElseThrow(RoleNotFoundException::new);
        if (!role.isSystem()) throw new ValidationException("Only system roles can have their permissions updated.");

        List<Permission> permissions = permissionRepository.findAllByCodes(requestedCodes);
        if (permissions.size() != requestedCodes.size() || permissions.stream().anyMatch(permission -> !permission.isActive())) {
            throw new ValidationException("One or more permissions do not exist or are inactive.");
        }

        User actor = userRepository.findById(currentUserPort.getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Current user was not found."));
        ensureActorRetainsRoleManagement(actor, role, requestedCodes);

        Set<String> before = role.getPermissions().stream().map(Permission::getCode).collect(java.util.stream.Collectors.toSet());
        role.replacePermissions(new HashSet<>(permissions));
        Role saved = roleRepository.save(role);

        auditLogRepository.save(AuditLog.create(actor.getId(), ActionType.UPDATE, ResourceType.ROLE, saved.getId(),
                auditDetail(saved.getName(), before, requestedCodes), null));
        return mapper.role(saved);
    }

    private void ensureActorRetainsRoleManagement(User actor, Role targetRole, Set<String> requestedCodes) {
        if (!actor.getRoleId().equals(targetRole.getId()) || userRepository.countActiveByRoleId(targetRole.getId()) != 1) return;
        if (!requestedCodes.containsAll(ROLE_MANAGEMENT_PERMISSIONS)) {
            throw new ValidationException("The only active administrator cannot remove their role management permissions.");
        }
    }

    private String auditDetail(String roleName, Set<String> before, Set<String> after) {
        Set<String> added = new HashSet<>(after); added.removeAll(before);
        Set<String> removed = new HashSet<>(before); removed.removeAll(after);
        return "{\"roleName\":\"%s\",\"before\":%s,\"after\":%s,\"added\":%s,\"removed\":%s}"
                .formatted(roleName, jsonArray(before), jsonArray(after), jsonArray(added), jsonArray(removed));
    }

    private String jsonArray(Set<String> values) {
        return values.stream().sorted().map(value -> "\"" + value + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}

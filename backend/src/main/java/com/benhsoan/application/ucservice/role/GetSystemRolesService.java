package com.benhsoan.application.ucservice.role;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.role.RolePermissionsResult;
import com.benhsoan.port.inbound.role.GetSystemRolesUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSystemRolesService implements GetSystemRolesUseCase {
    private final RoleRepository roleRepository;
    private final RolePermissionsResultMapper mapper;

    @Override public List<RolePermissionsResult> getSystemRoles() {
        return roleRepository.findAllSystemRoles().stream().map(mapper::role).toList();
    }
}

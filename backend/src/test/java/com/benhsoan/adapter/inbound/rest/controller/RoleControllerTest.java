package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.AfterEach;

import com.benhsoan.adapter.inbound.rest.mapper.RoleRestMapper;
import com.benhsoan.domain.auth.exception.LastAdministratorPermissionException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.inbound.role.GetPermissionCatalogUseCase;
import com.benhsoan.port.inbound.role.GetSystemRolesUseCase;
import com.benhsoan.port.inbound.role.UpdateRolePermissionsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, RequirePermissionAspect.class, PermissionEvaluator.class,
        RoleControllerTest.AspectConfiguration.class})
class RoleControllerTest {
    @Autowired MockMvc mvc;
    @MockBean GetSystemRolesUseCase getSystemRolesUseCase;
    @MockBean GetPermissionCatalogUseCase getPermissionCatalogUseCase;
    @MockBean UpdateRolePermissionsUseCase updateRolePermissionsUseCase;
    @MockBean RoleRestMapper mapper;
    @MockBean RoleRepository roleRepository;
    @MockBean AuditLogRepository auditLogRepository;
    @MockBean CurrentUserPort currentUserPort;
    @MockBean JwtTokenPort jwtTokenPort;
    @MockBean UserSessionRepository userSessionRepository;
    @MockBean UserRepository userRepository;
    @MockBean ClockPort clockPort;

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectConfiguration {
    }

    @AfterEach void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test void allowsEachEndpointWithRequiredPermission() throws Exception {
        mvc.perform(get("/roles").with(withPermission("PERMISSION_ROLE_READ")))
                .andExpect(status().isOk());
        mvc.perform(get("/permissions").with(withPermission("PERMISSION_PERMISSION_READ")))
                .andExpect(status().isOk());
        mvc.perform(put("/roles/{id}/permissions", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"permissionCodes\":[\"ROLE_READ\"]}")
                .with(withPermission("PERMISSION_ROLE_UPDATE")))
                .andExpect(status().isOk());
    }

    @Test void deniesMissingPermissions() throws Exception {
        mvc.perform(get("/roles").with(withPermission("PERMISSION_PATIENT_READ")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/permissions").with(withPermission("PERMISSION_PATIENT_READ")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/roles/{id}/permissions", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"permissionCodes\":[]}").with(withPermission("PERMISSION_PATIENT_READ")))
                .andExpect(status().isForbidden());
    }

    @Test void mapsLastAdministratorExceptionToConflict() throws Exception {
        when(updateRolePermissionsUseCase.updateRolePermissions(any())).thenThrow(new LastAdministratorPermissionException());
        mvc.perform(put("/roles/{id}/permissions", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"permissionCodes\":[\"ROLE_READ\"]}")
                .with(withPermission("PERMISSION_ROLE_UPDATE")))
                .andExpect(status().isConflict());
    }

    private RequestPostProcessor withPermission(String permission) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "u", null, List.of(new SimpleGrantedAuthority(permission))));
            return request;
        };
    }
}

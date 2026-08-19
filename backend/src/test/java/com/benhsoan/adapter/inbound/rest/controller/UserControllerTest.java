package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.benhsoan.adapter.inbound.rest.mapper.UserRestMapper;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.ActivateUserUseCase;
import com.benhsoan.port.inbound.user.CreateUserUseCase;
import com.benhsoan.port.inbound.user.DeactivateUserUseCase;
import com.benhsoan.port.inbound.user.GetAllUsersUseCase;
import com.benhsoan.port.inbound.user.GetDoctorsUseCase;
import com.benhsoan.port.inbound.user.GetUserUseCase;
import com.benhsoan.port.inbound.user.UpdateUserUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, UserRestMapper.class, RequirePermissionAspect.class,
        PermissionEvaluator.class, UserControllerTest.AspectConfiguration.class})
class UserControllerTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectConfiguration {
    }

    @Autowired
    private MockMvc mvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private CreateUserUseCase createUserUseCase;
    @org.springframework.boot.test.mock.mockito.MockBean
    private GetAllUsersUseCase getAllUsersUseCase;
    @org.springframework.boot.test.mock.mockito.MockBean
    private GetDoctorsUseCase getDoctorsUseCase;
    @org.springframework.boot.test.mock.mockito.MockBean
    private GetUserUseCase getUserUseCase;
    @org.springframework.boot.test.mock.mockito.MockBean
    private UpdateUserUseCase updateUserUseCase;
    @org.springframework.boot.test.mock.mockito.MockBean
    private ActivateUserUseCase activateUserUseCase;
    @org.springframework.boot.test.mock.mockito.MockBean
    private DeactivateUserUseCase deactivateUserUseCase;
    @org.springframework.boot.test.mock.mockito.MockBean
    private RoleRepository roleRepository;
    @org.springframework.boot.test.mock.mockito.MockBean
    private AuditLogRepository auditLogRepository;
    @org.springframework.boot.test.mock.mockito.MockBean
    private CurrentUserPort currentUserPort;
    @org.springframework.boot.test.mock.mockito.MockBean
    private JwtTokenPort jwtTokenPort;
    @org.springframework.boot.test.mock.mockito.MockBean
    private UserRepository userRepository;
    @org.springframework.boot.test.mock.mockito.MockBean
    private UserSessionRepository userSessionRepository;
    @org.springframework.boot.test.mock.mockito.MockBean
    private ClockPort clockPort;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsUserManagementActionsWithTheirRequiredPermissions() throws Exception {
        UUID userId = UUID.randomUUID();
        when(createUserUseCase.createUser(any())).thenReturn(result(userId));
        when(getAllUsersUseCase.getAll()).thenReturn(List.of(result(userId)));
        when(updateUserUseCase.update(any(), any())).thenReturn(result(userId));

        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(createRequest())
                .with(withPermission("PERMISSION_USER_CREATE")))
                .andExpect(status().isOk());
        mvc.perform(get("/users").with(withPermission("PERMISSION_USER_READ")))
                .andExpect(status().isOk());
        mvc.perform(put("/users/{id}", userId).contentType(MediaType.APPLICATION_JSON).content(updateRequest())
                .with(withPermission("PERMISSION_USER_UPDATE")))
                .andExpect(status().isOk());
    }

    @Test
    void deniesActionsWhenTheRequiredPermissionIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();

        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(createRequest())
                .with(withPermission("PERMISSION_USER_READ")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/users").with(withPermission("PERMISSION_USER_UPDATE")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/users/{id}", userId).contentType(MediaType.APPLICATION_JSON).content(updateRequest())
                .with(withPermission("PERMISSION_USER_READ")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(createUserUseCase, getAllUsersUseCase, updateUserUseCase);
    }

    private static UserResult result(UUID id) {
        return new UserResult(id, "user", "User", "user@example.com", "0900000000", "ADMIN", true);
    }

    private static String createRequest() {
        return """
                {"username":"user","password":"password","fullName":"User","email":"user@example.com","phone":"0900000000","roleName":"ADMIN"}
                """;
    }

    private static String updateRequest() {
        return """
                {"fullName":"User","email":"user@example.com","phone":"0900000000","roleName":"ADMIN"}
                """;
    }

    private RequestPostProcessor withPermission(String permission) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", null, List.of(new SimpleGrantedAuthority(permission))));
            return request;
        };
    }
}

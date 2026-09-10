package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.result.LoginAuditLogResult;
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

    @MockitoBean
    private CreateUserUseCase createUserUseCase;
    @MockitoBean
    private GetAllUsersUseCase getAllUsersUseCase;
    @MockitoBean
    private GetDoctorsUseCase getDoctorsUseCase;
    @MockitoBean
    private GetUserUseCase getUserUseCase;
    @MockitoBean
    private UpdateUserUseCase updateUserUseCase;
    @MockitoBean
    private ActivateUserUseCase activateUserUseCase;
    @MockitoBean
    private DeactivateUserUseCase deactivateUserUseCase;
    @MockitoBean
    private com.benhsoan.port.inbound.user.ResetPasswordUseCase resetPasswordUseCase;
    @MockitoBean
    private com.benhsoan.port.inbound.user.UnlockUserUseCase unlockUserUseCase;
    @MockitoBean
    private com.benhsoan.port.inbound.user.GetLoginAuditLogsUseCase getLoginAuditLogsUseCase;
    @MockitoBean
    private RoleRepository roleRepository;
    @MockitoBean
    private AuditLogRepository auditLogRepository;
    @MockitoBean
    private CurrentUserPort currentUserPort;
    @MockitoBean
    private JwtTokenPort jwtTokenPort;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserSessionRepository userSessionRepository;
    @MockitoBean
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
    void allowsPatientToRetrieveActiveDoctorsWithoutUserReadPermission() throws Exception {
        UUID doctorId = UUID.randomUUID();
        when(getDoctorsUseCase.getAllActiveDoctors()).thenReturn(List.of(result(doctorId)));

        mvc.perform(get("/users/doctors").with(user("patient").roles("PATIENT")))
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

    @Test
    void allowsResetPasswordOnlyWithUserResetPasswordPermission() throws Exception {
        UUID userId = UUID.randomUUID();
        when(resetPasswordUseCase.resetPassword(any())).thenReturn(
                new com.benhsoan.port.dto.result.ResetPasswordResult(userId, "user1", "TempPass123", java.time.Instant.now())
        );

        mvc.perform(post("/users/{id}/reset-password", userId)
                        .with(withPermission("PERMISSION_USER_RESET_PASSWORD")))
                .andExpect(status().isOk());
    }

    @Test
    void deniesResetPasswordWhenActorOnlyHasUserUpdatePermission() throws Exception {
        UUID userId = UUID.randomUUID();

        mvc.perform(post("/users/{id}/reset-password", userId)
                        .with(withPermission("PERMISSION_USER_UPDATE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(resetPasswordUseCase);
    }

    @Test
    void deniesResetPasswordWhenUserResetPermissionIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();

        mvc.perform(post("/users/{id}/reset-password", userId)
                        .with(withPermission("PERMISSION_PATIENT_READ")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(resetPasswordUseCase);
    }

    @Test
    void allowsUnlockWhenUserHasUpdatePermission() throws Exception {
        UUID userId = UUID.randomUUID();
        when(unlockUserUseCase.unlockUser(userId)).thenReturn(result(userId));

        mvc.perform(post("/users/{id}/unlock", userId)
                        .with(withPermission("PERMISSION_USER_UPDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void deniesUnlockWhenPermissionIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();

        mvc.perform(post("/users/{id}/unlock", userId)
                        .with(withPermission("PERMISSION_PATIENT_READ")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(unlockUserUseCase);
    }

    @Test
    void allowsGetLoginLogsWhenUserHasReadPermission() throws Exception {
        UUID userId = UUID.randomUUID();
        LoginAuditLogResult log = new LoginAuditLogResult(
                UUID.randomUUID(), userId, ActionType.LOCK, ResourceType.USER, userId,
                "{\"failedAttempts\":5}", null, Instant.now()
        );
        when(getLoginAuditLogsUseCase.getLoginAuditLogs(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mvc.perform(get("/users/{id}/login-logs", userId)
                        .with(withPermission("PERMISSION_USER_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actionType").value("LOCK"))
                .andExpect(jsonPath("$.content[0].resourceType").value("USER"));
    }

    @Test
    void deniesGetLoginLogsWhenPermissionIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();

        mvc.perform(get("/users/{id}/login-logs", userId)
                        .with(withPermission("PERMISSION_ROLE_READ")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getLoginAuditLogsUseCase);
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

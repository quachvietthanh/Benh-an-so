package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.UserRestMapper;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.user.ResetPasswordCommand;
import com.benhsoan.port.dto.result.ResetPasswordResult;
import com.benhsoan.port.inbound.user.ActivateUserUseCase;
import com.benhsoan.port.inbound.user.CreateUserUseCase;
import com.benhsoan.port.inbound.user.DeactivateUserUseCase;
import com.benhsoan.port.inbound.user.GetAllUsersUseCase;
import com.benhsoan.port.inbound.user.GetDoctorsUseCase;
import com.benhsoan.port.inbound.user.GetUserUseCase;
import com.benhsoan.port.inbound.user.ResetPasswordUseCase;
import com.benhsoan.port.inbound.user.UpdateUserUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        UserRestMapper.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        ResetPasswordSecurityIntegrationTest.AspectConfiguration.class
})
class ResetPasswordSecurityIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectConfiguration {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean private CreateUserUseCase createUserUseCase;
    @MockitoBean private GetAllUsersUseCase getAllUsersUseCase;
    @MockitoBean private GetDoctorsUseCase getDoctorsUseCase;
    @MockitoBean private GetUserUseCase getUserUseCase;
    @MockitoBean private UpdateUserUseCase updateUserUseCase;
    @MockitoBean private ActivateUserUseCase activateUserUseCase;
    @MockitoBean private DeactivateUserUseCase deactivateUserUseCase;
    @MockitoBean private ResetPasswordUseCase resetPasswordUseCase;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;

    private final UUID receptionistId = UUID.randomUUID();
    private final UUID targetStaffId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Admin with USER_RESET_PASSWORD resets staff password successfully (NCL-01-CN-005 TC-03)")
    void adminResetsStaffPasswordSuccessfully() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("PERMISSION_USER_RESET_PASSWORD")
                        )
                )
        );

        when(resetPasswordUseCase.resetPassword(any(ResetPasswordCommand.class))).thenReturn(
                new ResetPasswordResult(targetStaffId, "doctor1", "GeneratedTemp123", Instant.now())
        );

        mvc.perform(post("/users/{id}/reset-password", targetStaffId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(targetStaffId.toString()))
                .andExpect(jsonPath("$.username").value("doctor1"))
                .andExpect(jsonPath("$.temporaryPassword").value("GeneratedTemp123"));
    }

    @Test
    @DisplayName("Receptionist attempting to reset password is denied (403) and audited (NCL-01-CN-005 TC-04)")
    void receptionistResetPasswordDeniedAndAudited() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "receptionist1",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_RECEPTIONIST"),
                                new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"),
                                new SimpleGrantedAuthority("PERMISSION_APPOINTMENT_READ")
                        )
                )
        );

        mvc.perform(post("/users/{id}/reset-password", targetStaffId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(resetPasswordUseCase);

        // Verify ACCESS_DENIED was audited with the actor's ID
        verify(auditLogRepository).save(argThat(log ->
                log.getActionType() == ActionType.ACCESS_DENIED
                        && receptionistId.equals(log.getUserId())
        ));
    }
}

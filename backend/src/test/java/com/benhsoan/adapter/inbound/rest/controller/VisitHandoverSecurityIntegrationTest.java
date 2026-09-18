package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.VisitRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.CurrentUserAdapter;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.visit.HandoverPatientCommand;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.dto.result.VisitHandoverResult;
import com.benhsoan.port.inbound.user.GetDoctorsUseCase;
import com.benhsoan.port.inbound.visit.GetVisitEncounterUseCase;
import com.benhsoan.port.inbound.visit.GetVisitHandoversUseCase;
import com.benhsoan.port.inbound.visit.HandoverPatientUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = VisitController.class)
@Import({
        AnonymizationModeState.class,
        AopAutoConfiguration.class,
        VisitHandoverSecurityIntegrationTest.AspectTestConfig.class,
        VisitRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        CurrentUserAdapter.class
})
@DisplayName("VisitHandoverSecurityIntegrationTest - Security RBAC 401/403 Layer Tests (P2-2)")
class VisitHandoverSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GetVisitEncounterUseCase getVisitEncounterUseCase;
    @MockitoBean private HandoverPatientUseCase handoverPatientUseCase;
    @MockitoBean private GetVisitHandoversUseCase getVisitHandoversUseCase;
    @MockitoBean private GetDoctorsUseCase getDoctorsUseCase;
    @MockitoBean private com.benhsoan.port.inbound.visit.GetVisitSummaryUseCase getVisitSummaryUseCase;
    @MockitoBean private com.benhsoan.port.inbound.visit.ExportVisitSummaryUseCase exportVisitSummaryUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private final UUID visitId = UUID.randomUUID();
    private final UUID targetDoctorId = UUID.randomUUID();
    private final UUID fromDoctorId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-17T10:00:00Z");

    @Test
    @DisplayName("ST-01: Người dùng chưa xác thực gọi bàn giao -> HTTP 401 Unauthorized")
    void handover_anonymousUser_shouldReturn401() throws Exception {
        String payload = """
                {
                    "targetDoctorId": "%s",
                    "reason": "Chuyen ca truc"
                }
                """.formatted(targetDoctorId);

        mockMvc.perform(post("/visits/{visitId}/handover", visitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ST-02: Người dùng có vai trò RECEPTIONIST không có quyền MEDICAL_RECORD_HANDOVER -> HTTP 403 Forbidden")
    void handover_receptionistWithoutPermission_shouldReturn403() throws Exception {
        String payload = """
                {
                    "targetDoctorId": "%s",
                    "reason": "Chuyen ca truc"
                }
                """.formatted(targetDoctorId);

        mockMvc.perform(post("/visits/{visitId}/handover", visitId)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ST-03: Bác sĩ có quyền MEDICAL_RECORD_HANDOVER gọi bàn giao -> HTTP 200 OK")
    void handover_doctorWithPermission_shouldReturn200() throws Exception {
        VisitHandoverResult handoverResult = new VisitHandoverResult(
                UUID.randomUUID(), visitId, fromDoctorId, "Dr. Alice", targetDoctorId, "Dr. Bob",
                "Chuyen ca truc", now
        );

        when(handoverPatientUseCase.handover(eq(visitId), any(HandoverPatientCommand.class)))
                .thenReturn(handoverResult);

        String payload = """
                {
                    "targetDoctorId": "%s",
                    "reason": "Chuyen ca truc"
                }
                """.formatted(targetDoctorId);

        mockMvc.perform(post("/visits/{visitId}/handover", visitId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_HANDOVER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.fromDoctorName").value("Dr. Alice"))
                .andExpect(jsonPath("$.toDoctorName").value("Dr. Bob"))
                .andExpect(jsonPath("$.reason").value("Chuyen ca truc"));
    }

    @Test
    @DisplayName("ST-04: Người dùng chưa xác thực gọi xem lịch sử bàn giao -> HTTP 401 Unauthorized")
    void getHandovers_anonymousUser_shouldReturn401() throws Exception {
        mockMvc.perform(get("/visits/{visitId}/handovers", visitId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ST-05: Người dùng không có quyền MEDICAL_RECORD_READ gọi xem lịch sử -> HTTP 403 Forbidden")
    void getHandovers_userWithoutReadPermission_shouldReturn403() throws Exception {
        mockMvc.perform(get("/visits/{visitId}/handovers", visitId)
                        .with(user("patient").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ST-06: Endpoint /visits/handover/doctors trả về đúng HandoverDoctorResponse và không làm lộ email, phone")
    void getHandoverDoctors_shouldReturnOnlyIdAndFullNameWithoutSensitiveFields() throws Exception {
        UUID docId = UUID.randomUUID();
        UserResult doc = new UserResult(
                docId, "doctor1", "Dr. Doctor", "doctor@example.com", "0901234567", "DOCTOR", true
        );
        when(getDoctorsUseCase.getAllActiveDoctors()).thenReturn(List.of(doc));

        mockMvc.perform(get("/visits/handover/doctors")
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(docId.toString()))
                .andExpect(jsonPath("$[0].fullName").value("Dr. Doctor"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].username").doesNotExist());
    }

    @Test
    @DisplayName("ST-07: Migration V71 tuân thủ Least Privilege, không cấp USER_READ cho DOCTOR và không trùng version")
    void migrationV71_shouldFollowLeastPrivilegeAndProperVersioning() throws Exception {
        java.nio.file.Path migrationDir = java.nio.file.Path.of("src/main/resources/db/migration");
        org.junit.jupiter.api.Assertions.assertTrue(java.nio.file.Files.exists(migrationDir));

        // 1. Khong duoc trung lap version 67, 69, 70, 71
        long v67Count = java.nio.file.Files.list(migrationDir)
                .map(p -> p.getFileName().toString())
                .filter(name -> name.startsWith("V67__"))
                .count();
        org.junit.jupiter.api.Assertions.assertEquals(1, v67Count, "Phải chỉ có đúng 1 migration V67");

        long v69Count = java.nio.file.Files.list(migrationDir)
                .map(p -> p.getFileName().toString())
                .filter(name -> name.startsWith("V69__"))
                .count();
        org.junit.jupiter.api.Assertions.assertEquals(1, v69Count, "Phải chỉ có đúng 1 migration V69 từ PR #222");

        long v70Count = java.nio.file.Files.list(migrationDir)
                .map(p -> p.getFileName().toString())
                .filter(name -> name.startsWith("V70__"))
                .count();
        org.junit.jupiter.api.Assertions.assertEquals(1, v70Count, "Phải chỉ có đúng 1 migration V70 từ PR #227");

        // 2. Migration ban giao phai la V71
        java.nio.file.Path v71File = migrationDir.resolve("V71__create_visit_handover_tables.sql");
        org.junit.jupiter.api.Assertions.assertTrue(java.nio.file.Files.exists(v71File), "Migration bàn giao phải được đặt tên là V71");

        // 3. V71 khong duoc cap USER_READ
        String content = java.nio.file.Files.readString(v71File);
        org.junit.jupiter.api.Assertions.assertFalse(
                content.contains("USER_READ"),
                "Migration V71 vi phạm Least Privilege: không được cấp quyền USER_READ cho DOCTOR"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                content.contains("MEDICAL_RECORD_HANDOVER"),
                "Migration V71 phải cấp quyền MEDICAL_RECORD_HANDOVER"
        );
    }
}

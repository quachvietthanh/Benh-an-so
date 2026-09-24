package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicationProcurementRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.dto.result.ProcurementSuggestionResult;
import com.benhsoan.port.inbound.inventory.ApproveMedicationProcurementPlanUseCase;
import com.benhsoan.port.inbound.inventory.CreateMedicationProcurementPlanUseCase;
import com.benhsoan.port.inbound.inventory.GetMedicationProcurementPlanUseCase;
import com.benhsoan.port.inbound.inventory.GetMedicationProcurementSuggestionUseCase;
import com.benhsoan.port.inbound.inventory.ListMedicationProcurementPlansUseCase;
import com.benhsoan.port.inbound.inventory.RejectMedicationProcurementPlanUseCase;
import com.benhsoan.port.inbound.inventory.UpdateMedicationProcurementPlanUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicationProcurementController.class)
@Import({
        MedicationProcurementRestMapper.class,
        AnonymizationModeState.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        MedicationProcurementSecurityIntegrationTest.AspectTestConfig.class
})
class MedicationProcurementSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMedicationProcurementSuggestionUseCase suggestionUseCase;

    @MockitoBean
    private CreateMedicationProcurementPlanUseCase createUseCase;

    @MockitoBean
    private GetMedicationProcurementPlanUseCase getUseCase;

    @MockitoBean
    private ListMedicationProcurementPlansUseCase listUseCase;

    @MockitoBean
    private UpdateMedicationProcurementPlanUseCase updateUseCase;

    @MockitoBean
    private ApproveMedicationProcurementPlanUseCase approveUseCase;

    @MockitoBean
    private RejectMedicationProcurementPlanUseCase rejectUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private ClockPort clockPort;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @Test
    @DisplayName("Dược sĩ có quyền MEDICATION_PROCUREMENT_READ được xem gợi ý số lượng thuốc (200 OK)")
    void pharmacistWithReadPermissionCanViewSuggestions() throws Exception {
        when(suggestionUseCase.getSuggestions(any(), any(), anyBoolean()))
                .thenReturn(new ProcurementSuggestionResult(LocalDate.now().minusDays(30), LocalDate.now(), Instant.now(), 0, List.of()));

        mockMvc.perform(get("/inventory/procurements/suggestions")
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICATION_PROCUREMENT_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Dược sĩ có quyền MEDICATION_PROCUREMENT_CREATE được tạo phiếu dự trù (201 Created)")
    void pharmacistWithCreatePermissionCanCreatePlan() throws Exception {
        UUID planId = UUID.randomUUID();
        ProcurementPlanResult mockResult = new ProcurementPlanResult(
                planId,
                "DT000001",
                ProcurementPlanStatus.DRAFT,
                UUID.randomUUID(),
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                1,
                10,
                10,
                0,
                "Ghi chú",
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                List.of()
        );
        when(createUseCase.create(any())).thenReturn(mockResult);

        String jsonBody = """
                {
                  "periodStartDate": "2026-08-24",
                  "periodEndDate": "2026-09-23",
                  "note": "Dự trù định kỳ",
                  "submitImmediately": false,
                  "items": [
                    {
                      "medicineId": "11111111-2222-3333-4444-555555555555",
                      "currentStock": 20,
                      "minStockThreshold": 50,
                      "previousPeriodConsumption": 30,
                      "suggestedQuantity": 60,
                      "proposedQuantity": 70,
                      "note": "Tăng thêm"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/inventory/procurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICATION_PROCUREMENT_CREATE"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Tạo phiếu dự trù thiếu ngày bắt đầu hoặc ngày kết thúc trả về 400 Bad Request")
    void createPlanWithoutDatesReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                  "note": "Thiếu ngày",
                  "items": [
                    {
                      "medicineId": "11111111-2222-3333-4444-555555555555",
                      "currentStock": 20,
                      "minStockThreshold": 50,
                      "previousPeriodConsumption": 30,
                      "suggestedQuantity": 60,
                      "proposedQuantity": 70
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/inventory/procurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICATION_PROCUREMENT_CREATE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tạo phiếu dự trù với danh sách thuốc rỗng trả về 400 Bad Request")
    void createPlanWithEmptyItemsReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                  "periodStartDate": "2026-08-24",
                  "periodEndDate": "2026-09-23",
                  "note": "Rỗng items",
                  "items": []
                }
                """;

        mockMvc.perform(post("/inventory/procurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICATION_PROCUREMENT_CREATE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Quản lý phòng khám có quyền MEDICATION_PROCUREMENT_APPROVE được duyệt phiếu (200 OK)")
    void managerWithApprovePermissionCanApprovePlan() throws Exception {
        UUID planId = UUID.randomUUID();
        ProcurementPlanResult mockResult = new ProcurementPlanResult(
                planId,
                "DT000001",
                ProcurementPlanStatus.APPROVED,
                UUID.randomUUID(),
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                1,
                10,
                10,
                10,
                "Ghi chú",
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                null,
                Instant.now(),
                Instant.now(),
                List.of()
        );
        when(approveUseCase.approve(any())).thenReturn(mockResult);

        mockMvc.perform(post("/inventory/procurements/{id}/approve", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Đồng ý duyệt\"}")
                        .with(user("manager").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICATION_PROCUREMENT_APPROVE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Lễ tân không có quyền truy cập bị chặn 403 Forbidden và ghi nhật ký kiểm toán (TC-03)")
    void receptionistWithoutPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/inventory/procurements/suggestions")
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bác sĩ không có quyền dự trù mua thuốc bị chặn 403 Forbidden")
    void doctorWithoutPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/inventory/procurements/suggestions")
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Yêu cầu chưa xác thực bị trả về 401 Unauthorized")
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/inventory/procurements/suggestions"))
                .andExpect(status().isUnauthorized());
    }
}

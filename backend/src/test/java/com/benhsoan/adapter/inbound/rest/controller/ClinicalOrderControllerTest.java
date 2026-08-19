package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalOrderRestMapper;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.ClinicalOrderResult;
import com.benhsoan.port.inbound.clinical.CreateClinicalOrderUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalOrdersByVisitUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ClinicalOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ClinicalOrderRestMapper.class, GlobalExceptionHandler.class, RequirePermissionAspect.class,
        PermissionEvaluator.class, ClinicalOrderControllerTest.AspectTestConfig.class})
@DisplayName("ClinicalOrderController - MockMvc Tests")
class ClinicalOrderControllerTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateClinicalOrderUseCase createClinicalOrderUseCase;

    @MockitoBean
    private GetClinicalOrdersByVisitUseCase getClinicalOrdersByVisitUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private ClockPort clockPort;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /clinical-orders/visits/{id} - 201 Created")
    void createClinicalOrderReturns201() throws Exception {
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(createClinicalOrderUseCase.createOrder(eq(visitId), any()))
                .thenReturn(new ClinicalOrderResult(
                        UUID.randomUUID(), "ORD-123", visitId, UUID.randomUUID(), UUID.randomUUID(),
                        "Clinical reason", "ORDERED", Instant.parse("2026-08-20T01:00:00Z"), null, List.of()
                ));

        String body = """
                {
                    "clinicalReason": "Check up",
                    "items": [
                        {
                            "serviceId": "%s"
                        }
                    ]
                }
                """.formatted(serviceId);

        mockMvc.perform(post("/clinical-orders/visits/{visitId}", visitId)
                        .with(withPermission("CLINICAL_ORDER_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderCode").value("ORD-123"))
                .andExpect(jsonPath("$.status").value("ORDERED"));
    }

    @Test
    @DisplayName("POST /clinical-orders/visits/{id} - 400 when items empty")
    void createClinicalOrderRejectsEmptyItems() throws Exception {
        UUID visitId = UUID.randomUUID();
        String body = """
                {
                    "clinicalReason": "Check up",
                    "items": []
                }
                """;

        mockMvc.perform(post("/clinical-orders/visits/{visitId}", visitId)
                        .with(withPermission("CLINICAL_ORDER_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /clinical-orders/visits/{id} - 200 OK")
    void getsClinicalOrdersByVisit() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(getClinicalOrdersByVisitUseCase.getOrdersByVisit(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(new ClinicalOrderResult(
                        UUID.randomUUID(), "ORD-123", visitId, UUID.randomUUID(), UUID.randomUUID(),
                        "Clinical reason", "ORDERED", Instant.parse("2026-08-20T01:00:00Z"), null, List.of()
                ))));

        mockMvc.perform(get("/clinical-orders/visits/{visitId}", visitId)
                        .with(withPermission("CLINICAL_ORDER_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderCode").value("ORD-123"));
    }

    @Test
    void rejectsMissingClinicalOrderPermission() throws Exception {
        mockMvc.perform(get("/clinical-orders/visits/{visitId}", UUID.randomUUID())
                        .with(withPermission("CLINICAL_ORDER_CREATE")))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor withPermission(String permission) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "snapshot-user", null, List.of(new SimpleGrantedAuthority("PERMISSION_" + permission))));
            return request;
        };
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.benhsoan.adapter.inbound.rest.mapper.BillingRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.domain.billing.exception.SelfApprovalNotAllowedException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.billing.CreateDiscountRequestCommand;
import com.benhsoan.port.dto.command.billing.RejectDiscountRequestCommand;
import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.inbound.billing.ApproveDiscountRequestUseCase;
import com.benhsoan.port.inbound.billing.CreateDiscountRequestUseCase;
import com.benhsoan.port.inbound.billing.GetDiscountRequestsUseCase;
import com.benhsoan.port.inbound.billing.RejectDiscountRequestUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = DiscountRequestController.class)
@Import({
        AnonymizationModeState.class,
        BillingRestMapper.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        DiscountSecurityIntegrationTest.AspectTestConfig.class
})
class DiscountSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CreateDiscountRequestUseCase createDiscountRequestUseCase;
    @MockitoBean private ApproveDiscountRequestUseCase approveDiscountRequestUseCase;
    @MockitoBean private RejectDiscountRequestUseCase rejectDiscountRequestUseCase;
    @MockitoBean private GetDiscountRequestsUseCase getDiscountRequestsUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    private static RequestPostProcessor permission(String role, String... codes) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        for (String code : codes) {
            authorities.add(new SimpleGrantedAuthority("PERMISSION_" + code));
        }
        return user("tester").roles(role).authorities(authorities);
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/invoices/discount-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsCreateWhenUserHasInvoiceCreatePermission() throws Exception {
        UUID visitId = UUID.randomUUID();
        DiscountRequestResult result = sampleResult(UUID.randomUUID(), visitId, DiscountRequestStatus.PENDING);
        when(createDiscountRequestUseCase.create(any(CreateDiscountRequestCommand.class))).thenReturn(result);

        mockMvc.perform(post("/invoices/discount-requests")
                        .with(permission("RECEPTIONIST", "INVOICE_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "visitId": "%s",
                            "discountType": "PERCENTAGE",
                            "discountValue": 10.00,
                            "reason": "Uu dai"
                        }
                        """.formatted(visitId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void deniesCreateWhenUserLacksInvoiceCreatePermission() throws Exception {
        mockMvc.perform(post("/invoices/discount-requests")
                        .with(permission("PHARMACIST", "PRESCRIPTION_READ"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "visitId": "%s",
                            "discountType": "PERCENTAGE",
                            "discountValue": 10.00,
                            "reason": "Uu dai"
                        }
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsApproveWhenUserHasInvoiceUpdatePermission() throws Exception {
        UUID id = UUID.randomUUID();
        DiscountRequestResult result = sampleResult(id, UUID.randomUUID(), DiscountRequestStatus.APPROVED);
        when(approveDiscountRequestUseCase.approve(id)).thenReturn(result);

        mockMvc.perform(post("/invoices/discount-requests/{id}/approve", id)
                        .with(permission("MANAGER", "INVOICE_UPDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void deniesApproveWhenUserLacksInvoiceUpdatePermission() throws Exception {
        mockMvc.perform(post("/invoices/discount-requests/{id}/approve", UUID.randomUUID())
                        .with(permission("DOCTOR", "INVOICE_READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    void returns403WhenSelfApprovalAttempted() throws Exception {
        UUID id = UUID.randomUUID();
        when(approveDiscountRequestUseCase.approve(id))
                .thenThrow(new SelfApprovalNotAllowedException());

        mockMvc.perform(post("/invoices/discount-requests/{id}/approve", id)
                        .with(permission("ADMIN", "INVOICE_UPDATE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SELF_APPROVAL_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("Người đề nghị giảm giá không được tự phê duyệt đề nghị của chính mình."));
    }

    @Test
    void allowsRejectWhenUserHasInvoiceUpdatePermission() throws Exception {
        UUID id = UUID.randomUUID();
        DiscountRequestResult result = sampleResult(id, UUID.randomUUID(), DiscountRequestStatus.REJECTED);
        when(rejectDiscountRequestUseCase.reject(any(RejectDiscountRequestCommand.class))).thenReturn(result);

        mockMvc.perform(post("/invoices/discount-requests/{id}/reject", id)
                        .with(permission("MANAGER", "INVOICE_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "rejectionReason": "Không đủ điều kiện"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void allowsSearchWhenUserHasInvoiceReadPermission() throws Exception {
        when(getDiscountRequestsUseCase.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/invoices/discount-requests")
                        .with(permission("RECEPTIONIST", "INVOICE_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    private static DiscountRequestResult sampleResult(UUID id, UUID visitId, DiscountRequestStatus status) {
        return new DiscountRequestResult(
                id,
                visitId,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("90000.00"),
                "Uu dai",
                status,
                UUID.randomUUID(),
                Instant.now(),
                status == DiscountRequestStatus.APPROVED ? UUID.randomUUID() : null,
                status == DiscountRequestStatus.APPROVED ? Instant.now() : null,
                status == DiscountRequestStatus.REJECTED ? UUID.randomUUID() : null,
                status == DiscountRequestStatus.REJECTED ? "Không đủ điều kiện" : null,
                status == DiscountRequestStatus.REJECTED ? Instant.now() : null,
                null
        );
    }
}

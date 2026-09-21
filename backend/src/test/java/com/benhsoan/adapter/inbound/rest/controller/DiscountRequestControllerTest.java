package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.BillingRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.port.dto.command.billing.CreateDiscountRequestCommand;
import com.benhsoan.port.dto.command.billing.RejectDiscountRequestCommand;
import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.inbound.billing.ApproveDiscountRequestUseCase;
import com.benhsoan.port.inbound.billing.CreateDiscountRequestUseCase;
import com.benhsoan.port.inbound.billing.GetDiscountRequestsUseCase;
import com.benhsoan.port.inbound.billing.RejectDiscountRequestUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = DiscountRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({BillingRestMapper.class, AnonymizationModeState.class, GlobalExceptionHandler.class})
class DiscountRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CreateDiscountRequestUseCase createDiscountRequestUseCase;
    @MockitoBean private ApproveDiscountRequestUseCase approveDiscountRequestUseCase;
    @MockitoBean private RejectDiscountRequestUseCase rejectDiscountRequestUseCase;
    @MockitoBean private GetDiscountRequestsUseCase getDiscountRequestsUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void createsDiscountRequestSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T10:00:00Z");

        DiscountRequestResult result = new DiscountRequestResult(
                id,
                visitId,
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("80000.00"),
                "Hỗ trợ bệnh nhân nghèo",
                DiscountRequestStatus.PENDING,
                requesterId,
                now,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(createDiscountRequestUseCase.create(any(CreateDiscountRequestCommand.class)))
                .thenReturn(result);

        mockMvc.perform(post("/invoices/discount-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "visitId": "%s",
                            "discountType": "PERCENTAGE",
                            "discountValue": 20.00,
                            "originalAmount": 100000.00,
                            "reason": "Hỗ trợ bệnh nhân nghèo"
                        }
                        """.formatted(visitId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.discountAmount").value(20000.00))
                .andExpect(jsonPath("$.finalAmount").value(80000.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsCreationWithBlankReason() throws Exception {
        mockMvc.perform(post("/invoices/discount-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "visitId": "%s",
                            "discountType": "PERCENTAGE",
                            "discountValue": 20.00,
                            "reason": "   "
                        }
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchesDiscountRequests() throws Exception {
        UUID id = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        DiscountRequestResult item = new DiscountRequestResult(
                id,
                visitId,
                DiscountType.FULL_FREE,
                new BigDecimal("100.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                "Miễn phí 100%",
                DiscountRequestStatus.PENDING,
                UUID.randomUUID(),
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(getDiscountRequestsUseCase.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/invoices/discount-requests")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].discountType").value("FULL_FREE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getsDiscountRequestById() throws Exception {
        UUID id = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        DiscountRequestResult result = new DiscountRequestResult(
                id,
                visitId,
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("50000.00"),
                new BigDecimal("200000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("150000.00"),
                "Giảm 50k",
                DiscountRequestStatus.PENDING,
                UUID.randomUUID(),
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(getDiscountRequestsUseCase.getById(id)).thenReturn(result);

        mockMvc.perform(get("/invoices/discount-requests/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.discountValue").value(50000.00));
    }

    @Test
    void approvesDiscountRequest() throws Exception {
        UUID id = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        Instant now = Instant.now();

        DiscountRequestResult result = new DiscountRequestResult(
                id,
                UUID.randomUUID(),
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("80000.00"),
                "Lý do",
                DiscountRequestStatus.APPROVED,
                UUID.randomUUID(),
                now.minusSeconds(3600),
                approverId,
                now,
                null,
                null,
                null,
                null
        );

        when(approveDiscountRequestUseCase.approve(id)).thenReturn(result);

        mockMvc.perform(post("/invoices/discount-requests/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value(approverId.toString()));
    }

    @Test
    void rejectsDiscountRequest() throws Exception {
        UUID id = UUID.randomUUID();
        UUID rejecterId = UUID.randomUUID();
        Instant now = Instant.now();

        DiscountRequestResult result = new DiscountRequestResult(
                id,
                UUID.randomUUID(),
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("80000.00"),
                "Lý do",
                DiscountRequestStatus.REJECTED,
                UUID.randomUUID(),
                now.minusSeconds(3600),
                null,
                null,
                rejecterId,
                "Không đủ điều kiện",
                now,
                null
        );

        when(rejectDiscountRequestUseCase.reject(any(RejectDiscountRequestCommand.class))).thenReturn(result);

        mockMvc.perform(post("/invoices/discount-requests/{id}/reject", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "rejectionReason": "Không đủ điều kiện"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Không đủ điều kiện"));
    }

    @Test
    void rejectsSearchWhenFromIsAfterTo() throws Exception {
        mockMvc.perform(get("/invoices/discount-requests")
                        .param("requestedFrom", "2026-09-22T00:00:00Z")
                        .param("requestedTo", "2026-09-20T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Thời gian bắt đầu tìm kiếm phải trước hoặc bằng thời gian kết thúc."));
    }

    @Test
    void rejectsSearchWithInvalidPagination() throws Exception {
        mockMvc.perform(get("/invoices/discount-requests")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Chỉ số trang không được âm và kích thước trang phải từ 1 đến 100."));

        mockMvc.perform(get("/invoices/discount-requests")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Chỉ số trang không được âm và kích thước trang phải từ 1 đến 100."));
    }
}

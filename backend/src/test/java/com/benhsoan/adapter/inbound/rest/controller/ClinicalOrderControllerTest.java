package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalOrderRestMapper;
import com.benhsoan.port.dto.result.ClinicalOrderResult;
import com.benhsoan.port.inbound.clinical.CreateClinicalOrderUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ClinicalOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ClinicalOrderRestMapper.class)
@DisplayName("ClinicalOrderController - MockMvc Tests")
class ClinicalOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateClinicalOrderUseCase createClinicalOrderUseCase;

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

    @Test
    @DisplayName("POST /clinical-orders/visits/{id} - 200 OK")
    void createClinicalOrderReturns200() throws Exception {
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

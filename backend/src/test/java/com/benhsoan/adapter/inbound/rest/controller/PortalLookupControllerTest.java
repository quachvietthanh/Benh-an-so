package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.domain.portal.exception.PortalLookupNotFoundException;
import com.benhsoan.port.dto.query.portal.LookupPortalResultQuery;
import com.benhsoan.port.dto.result.portal.PortalLookupResult;
import com.benhsoan.port.dto.result.portal.PortalLookupResult.DiagnosisItem;
import com.benhsoan.port.inbound.portal.LookupPortalResultUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PortalLookupController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortalLookupControllerTest {

    private static final String CODE = "APPT-2026-0001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LookupPortalResultUseCase lookupPortalResultUseCase;

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
    void returnsPortalResult() throws Exception {
        when(lookupPortalResultUseCase.lookup(new LookupPortalResultQuery(CODE, null)))
                .thenReturn(result());

        mockMvc.perform(get("/portal/lookup").param("code", CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentCode").value(CODE))
                .andExpect(jsonPath("$.patientName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.doctorName").value("Bac Sy B"))
                .andExpect(jsonPath("$.diagnoses[0].name").value("Viem hong"));
    }

    @Test
    void returnsNotFoundForUnknownCode() throws Exception {
        when(lookupPortalResultUseCase.lookup(new LookupPortalResultQuery(CODE, null)))
                .thenThrow(new PortalLookupNotFoundException());

        mockMvc.perform(get("/portal/lookup").param("code", CODE))
                .andExpect(status().isNotFound());
    }

    private PortalLookupResult result() {
        return new PortalLookupResult(
                CODE,
                Instant.parse("2026-08-15T08:00:00Z"),
                "Kham tong quat",
                "Nguyen Van A",
                LocalDate.of(1990, 1, 1),
                "MALE",
                "091***001",
                "VISIT-001",
                Instant.parse("2026-08-15T08:30:00Z"),
                "Bac Sy B",
                List.of(new DiagnosisItem("D001", "Viem hong", "PRIMARY")),
                "Ket luan kham",
                "Uong thuoc theo don",
                List.of(),
                List.of()
        );
    }
}

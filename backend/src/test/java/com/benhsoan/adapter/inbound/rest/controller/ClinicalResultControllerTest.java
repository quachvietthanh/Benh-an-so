package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalResultRestMapper;
import com.benhsoan.domain.clinical.exception.ClinicalResultNotFoundException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.port.inbound.clinical.EnterClinicalResultUseCase;
import com.benhsoan.port.inbound.clinical.FinalizeClinicalResultUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalResultHistoryUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalResultUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalResultsByVisitUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalResultUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ClinicalResultController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ClinicalResultRestMapper.class, GlobalExceptionHandler.class})
class ClinicalResultControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private EnterClinicalResultUseCase enterClinicalResultUseCase;
    @MockitoBean private UpdateClinicalResultUseCase updateClinicalResultUseCase;
    @MockitoBean private FinalizeClinicalResultUseCase finalizeClinicalResultUseCase;
    @MockitoBean private GetClinicalResultUseCase getClinicalResultUseCase;
    @MockitoBean private GetClinicalResultsByVisitUseCase getClinicalResultsByVisitUseCase;
    @MockitoBean private GetClinicalResultHistoryUseCase getClinicalResultHistoryUseCase;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void returnsTheCommonNotFoundContractForAnUnknownClinicalResult() throws Exception {
        UUID clinicalResultId = UUID.randomUUID();
        when(getClinicalResultUseCase.getById(clinicalResultId))
                .thenThrow(new ClinicalResultNotFoundException(clinicalResultId));

        mockMvc.perform(get("/clinical-results/{id}", clinicalResultId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value("CLINICAL_RESULT_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/clinical-results/" + clinicalResultId))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void hidesBrokenInternalReferencesBehindTheCommonInternalErrorContract() throws Exception {
        UUID clinicalResultId = UUID.randomUUID();
        when(getClinicalResultUseCase.getById(clinicalResultId))
                .thenThrow(new IllegalStateException("Clinical data integrity failure: missing visit secret-id"));

        mockMvc.perform(get("/clinical-results/{id}", clinicalResultId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Internal server error."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-id"))))
                .andExpect(jsonPath("$.details").isMap());
    }
}

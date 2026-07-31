package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalServiceCatalogRestMapper;
import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.port.dto.result.ClinicalServiceCatalogResult;
import com.benhsoan.port.inbound.clinical.SearchClinicalServiceCatalogUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ClinicalServiceCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ClinicalServiceCatalogRestMapper.class)
class ClinicalServiceCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchClinicalServiceCatalogUseCase searchClinicalServiceCatalogUseCase;

    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void returnsActiveClinicalServices() throws Exception {
        when(searchClinicalServiceCatalogUseCase.search(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(new ClinicalServiceCatalogResult(
                        UUID.randomUUID(), "LAB-GLU", "Blood glucose", ClinicalServiceType.LAB_TEST,
                        ClinicalResultDataType.NUMBER, "mmol/L", "3.9-5.5", null,
                        Instant.parse("2026-08-20T01:00:00Z"), null
                ))));

        mockMvc.perform(get("/clinical-services").param("keyword", "glucose"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].serviceCode").value("LAB-GLU"));
    }
}

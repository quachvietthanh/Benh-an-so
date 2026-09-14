package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.DiagnosisCatalogRestMapper;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.dto.result.DiagnosisSuggestionResult;
import com.benhsoan.port.inbound.medicalrecord.GetDiagnosisCatalogUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetDiagnosisSuggestionsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = DiagnosisCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DiagnosisCatalogRestMapper.class)
@DisplayName("DiagnosisCatalogController - MockMvc Tests")
class DiagnosisCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetDiagnosisCatalogUseCase getDiagnosisCatalogUseCase;

    @MockitoBean
    private GetDiagnosisSuggestionsUseCase getDiagnosisSuggestionsUseCase;

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

    private final UUID id = UUID.randomUUID();

    @Test
    @DisplayName("GET /diagnosis-catalog?search=cold returns results")
    void searchReturnsResults() throws Exception {
        when(getDiagnosisCatalogUseCase.search("cold", null))
                .thenReturn(List.of(new DiagnosisCatalogResult(
                        id, "J00", "Common cold", null, "Respiratory", "Desc", true, Instant.now(), null)));

        mockMvc.perform(get("/diagnosis-catalog")
                        .param("search", "cold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("J00"))
                .andExpect(jsonPath("$[0].name").value("Common cold"))
                .andExpect(jsonPath("$[0].diseaseGroup").value("Respiratory"));
    }

    @Test
    @DisplayName("GET /diagnosis-catalog without search returns empty")
    void searchWithoutParam() throws Exception {
        when(getDiagnosisCatalogUseCase.search(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/diagnosis-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /diagnosis-catalog?search= returns empty")
    void searchEmptyParam() throws Exception {
        when(getDiagnosisCatalogUseCase.search("", null)).thenReturn(List.of());

        mockMvc.perform(get("/diagnosis-catalog")
                        .param("search", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /diagnosis-catalog?diseaseGroup= filters by group")
    void searchByDiseaseGroup() throws Exception {
        when(getDiagnosisCatalogUseCase.search(null, "Hệ hô hấp"))
                .thenReturn(List.of(new DiagnosisCatalogResult(
                        id, "J00", "Common cold", null, "Hệ hô hấp", "Desc", true, Instant.now(), null)));

        mockMvc.perform(get("/diagnosis-catalog")
                        .param("diseaseGroup", "Hệ hô hấp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("J00"))
                .andExpect(jsonPath("$[0].diseaseGroup").value("Hệ hô hấp"));
    }

    @Test
    @DisplayName("GET /diagnosis-catalog/suggestions returns recent, popular and disease groups")
    void suggestionsReturnsStructuredResponse() throws Exception {
        var recent = new DiagnosisCatalogResult(id, "J02.9", "Viêm họng cấp", null, "Hệ hô hấp", null, true, Instant.now(), null);
        when(getDiagnosisSuggestionsUseCase.suggest())
                .thenReturn(new DiagnosisSuggestionResult(List.of(recent), List.of(), List.of("Hệ hô hấp")));

        mockMvc.perform(get("/diagnosis-catalog/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recent[0].code").value("J02.9"))
                .andExpect(jsonPath("$.recent[0].name").value("Viêm họng cấp"))
                .andExpect(jsonPath("$.popular").isArray())
                .andExpect(jsonPath("$.popular").isEmpty())
                .andExpect(jsonPath("$.diseaseGroups[0]").value("Hệ hô hấp"));
    }
}

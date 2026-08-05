package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionRestMapper;
import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.inbound.prescription.CreatePrescriptionUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PrescriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PrescriptionRestMapper.class)
@DisplayName("PrescriptionController - MockMvc Tests")
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePrescriptionUseCase createPrescriptionUseCase;

    @MockitoBean
    private CheckDrugInteractionUseCase checkDrugInteractionUseCase;

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
    @DisplayName("POST /prescriptions/check-interactions - 200 OK with sorted warnings")
    void checkInteractionsReturnsWarnings() throws Exception {
        UUID aspirinId = UUID.randomUUID();
        UUID warfarinId = UUID.randomUUID();

        when(checkDrugInteractionUseCase.check(any()))
                .thenReturn(List.of(
                        new DrugInteractionWarningResult(
                                UUID.randomUUID(),
                                aspirinId,
                                warfarinId,
                                InteractionSeverity.SEVERE,
                                "Aspirin làm tăng nguy cơ chảy máu khi phối hợp với Warfarin.",
                                "Cân nhắc ngừng Aspirin."
                        )
                ));

        mockMvc.perform(post("/prescriptions/check-interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"drugIds":["%s","%s"]}
                                """.formatted(aspirinId, warfarinId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].drugIdA").value(aspirinId.toString()))
                .andExpect(jsonPath("$[0].drugIdB").value(warfarinId.toString()))
                .andExpect(jsonPath("$[0].severity").value("SEVERE"))
                .andExpect(jsonPath("$[0].description").value(
                        "Aspirin làm tăng nguy cơ chảy máu khi phối hợp với Warfarin."))
                .andExpect(jsonPath("$[0].clinicalRecommendation").value(
                        "Cân nhắc ngừng Aspirin."));
    }

    @Test
    @DisplayName("POST /prescriptions/check-interactions - 200 OK with empty array when no interaction")
    void checkInteractionsReturnsEmptyArray() throws Exception {
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());

        mockMvc.perform(post("/prescriptions/check-interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"drugIds":["%s","%s"]}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST /prescriptions/check-interactions - 400 when drugIds is empty")
    void checkInteractionsRejectsEmptyDrugIds() throws Exception {
        mockMvc.perform(post("/prescriptions/check-interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"drugIds":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /prescriptions/check-interactions - 400 when drugIds is missing")
    void checkInteractionsRejectsMissingDrugIds() throws Exception {
        mockMvc.perform(post("/prescriptions/check-interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }
}

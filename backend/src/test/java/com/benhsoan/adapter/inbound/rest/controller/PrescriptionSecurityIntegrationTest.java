package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.inbound.prescription.AmendPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CancelPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.inbound.prescription.CreatePrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.DispensePrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionsByMedicalRecordUseCase;
import com.benhsoan.port.inbound.prescription.SearchPrescriptionsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PrescriptionController.class)
@Import({PrescriptionRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class PrescriptionSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreatePrescriptionUseCase createPrescriptionUseCase;
    @MockitoBean private AmendPrescriptionUseCase amendPrescriptionUseCase;
    @MockitoBean private GetPrescriptionUseCase getPrescriptionUseCase;
    @MockitoBean private GetPrescriptionsByMedicalRecordUseCase getPrescriptionsByMedicalRecordUseCase;
    @MockitoBean private SearchPrescriptionsUseCase searchPrescriptionsUseCase;
    @MockitoBean private DispensePrescriptionUseCase dispensePrescriptionUseCase;
    @MockitoBean private CancelPrescriptionUseCase cancelPrescriptionUseCase;
    @MockitoBean private CheckDrugInteractionUseCase checkDrugInteractionUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;

    @Test
    void onlyAllowsPharmacistsAndAdminsToReadDispensingQueue() throws Exception {
        when(searchPrescriptionsUseCase.search(any())).thenReturn(Page.empty());

        for (String role : new String[] {"ADMIN", "PHARMACIST"}) {
            mockMvc.perform(get("/prescriptions")
                            .param("status", "PENDING_DISPENSE")
                            .with(user("tester").roles(role)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/prescriptions")
                        .param("status", "PENDING_DISPENSE")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }
}

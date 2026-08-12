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

import com.benhsoan.adapter.inbound.rest.mapper.MedicineRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.inbound.medicine.ActivateMedicineUseCase;
import com.benhsoan.port.inbound.medicine.CreateMedicineUseCase;
import com.benhsoan.port.inbound.medicine.DeactivateMedicineUseCase;
import com.benhsoan.port.inbound.medicine.GetMedicineUseCase;
import com.benhsoan.port.inbound.medicine.SearchMedicinesUseCase;
import com.benhsoan.port.inbound.medicine.UpdateMedicineUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicineController.class)
@Import({MedicineRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class MedicineSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateMedicineUseCase createMedicineUseCase;
    @MockitoBean private UpdateMedicineUseCase updateMedicineUseCase;
    @MockitoBean private ActivateMedicineUseCase activateMedicineUseCase;
    @MockitoBean private DeactivateMedicineUseCase deactivateMedicineUseCase;
    @MockitoBean private GetMedicineUseCase getMedicineUseCase;
    @MockitoBean private SearchMedicinesUseCase searchMedicinesUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;

    @Test
    void allowsDoctorsAdminsAndPharmacistsToReadMedicineCatalog() throws Exception {
        when(searchMedicinesUseCase.search(any())).thenReturn(Page.empty());

        for (String role : new String[] {"ADMIN", "DOCTOR", "PHARMACIST"}) {
            mockMvc.perform(get("/medicines")
                            .param("active", "true")
                            .with(user("tester").roles(role)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void forbidsReceptionistFromReadingMedicineCatalog() throws Exception {
        when(searchMedicinesUseCase.search(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/medicines")
                        .param("active", "true")
                        .with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }
}

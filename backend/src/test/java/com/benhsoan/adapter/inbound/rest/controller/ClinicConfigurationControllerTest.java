package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicConfigurationRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.command.clinic.UpdateClinicConfigurationCommand;
import com.benhsoan.port.dto.result.clinic.ClinicConfigurationResult;
import com.benhsoan.port.inbound.clinic.GetClinicConfigurationUseCase;
import com.benhsoan.port.inbound.clinic.UpdateClinicConfigurationUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ClinicConfigurationController.class)
@Import({
        ClinicConfigurationRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class ClinicConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetClinicConfigurationUseCase getClinicConfigurationUseCase;
    @MockitoBean
    private UpdateClinicConfigurationUseCase updateClinicConfigurationUseCase;
    @MockitoBean
    private JwtTokenPort jwtTokenPort;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserSessionRepository userSessionRepository;
    @MockitoBean
    private ClockPort clockPort;

    @Test
    void adminGetsAndUpdatesClinicConfiguration() throws Exception {
        when(getClinicConfigurationUseCase.get()).thenReturn(result());
        when(updateClinicConfigurationUseCase.update(any())).thenReturn(result());

        mockMvc.perform(get("/system/clinic")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clinicName").value("Phong kham Benh So An"))
                .andExpect(jsonPath("$.openingTime").value("08:00:00"));

        mockMvc.perform(put("/system/clinic")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closingTime").value("17:00:00"));

        verify(updateClinicConfigurationUseCase).update(any(UpdateClinicConfigurationCommand.class));
    }

    @Test
    void rejectsARequestWithoutClinicName() throws Exception {
        mockMvc.perform(put("/system/clinic")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clinicName":" ","openingTime":"08:00:00","closingTime":"17:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.clinicName").value("Clinic name is required."));

        verifyNoInteractions(updateClinicConfigurationUseCase);
    }

    @Test
    void rejectsNonAdminAccess() throws Exception {
        mockMvc.perform(get("/system/clinic")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/system/clinic")
                        .with(user("doctor").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getClinicConfigurationUseCase, updateClinicConfigurationUseCase);
    }

    private static String requestBody() {
        return """
                {
                  "clinicName":"Phong kham Benh So An",
                  "address":"Thanh pho Ho Chi Minh",
                  "phone":"0900000000",
                  "openingTime":"08:00:00",
                  "closingTime":"17:00:00"
                }
                """;
    }

    private static ClinicConfigurationResult result() {
        return new ClinicConfigurationResult(
                "Phong kham Benh So An",
                "Thanh pho Ho Chi Minh",
                "0900000000",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );
    }
}

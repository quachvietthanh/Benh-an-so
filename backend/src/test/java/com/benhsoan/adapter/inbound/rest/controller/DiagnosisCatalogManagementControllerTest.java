package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.DiagnosisCatalogRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogCodeAlreadyExistsException;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogDeletionNotAllowedException;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogInUseException;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.medicalrecord.CreateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.command.medicalrecord.UpdateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.CreateDiagnosisCatalogUseCase;
import com.benhsoan.port.inbound.medicalrecord.DeleteDiagnosisCatalogUseCase;
import com.benhsoan.port.inbound.medicalrecord.DiagnosisCatalogManagementQueryUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateDiagnosisCatalogStatusUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateDiagnosisCatalogUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = DiagnosisCatalogManagementController.class)
@Import({
        AopAutoConfiguration.class,
        DiagnosisCatalogManagementControllerTest.AspectTestConfig.class,
        DiagnosisCatalogRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class DiagnosisCatalogManagementControllerTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    private static final UUID CATALOG_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-08-25T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private DiagnosisCatalogManagementQueryUseCase queryUseCase;
    @MockitoBean private CreateDiagnosisCatalogUseCase createUseCase;
    @MockitoBean private UpdateDiagnosisCatalogUseCase updateUseCase;
    @MockitoBean private UpdateDiagnosisCatalogStatusUseCase updateStatusUseCase;
    @MockitoBean private DeleteDiagnosisCatalogUseCase deleteUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void searchesAndGetsCatalogEntriesForCatalogManagers() throws Exception {
        when(queryUseCase.search("resp", false)).thenReturn(List.of(result(false)));
        when(queryUseCase.getById(CATALOG_ID)).thenReturn(result(false));

        mockMvc.perform(get("/system/diagnosis-catalog")
                        .param("keyword", "resp")
                        .param("active", "false")
                        .with(withManagePermission()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("J00"))
                .andExpect(jsonPath("$[0].diseaseGroup").value("Respiratory"))
                .andExpect(jsonPath("$[0].active").value(false));

        mockMvc.perform(get("/system/diagnosis-catalog/{id}", CATALOG_ID)
                        .with(withManagePermission()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATALOG_ID.toString()));
    }

    @Test
    void createsAndMapsRequestToCommand() throws Exception {
        when(createUseCase.create(any())).thenReturn(result(true));

        mockMvc.perform(post("/system/diagnosis-catalog")
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":" j00 ","name":"Common cold","diseaseGroup":"Respiratory","description":"Desc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/system/diagnosis-catalog/" + CATALOG_ID))
                .andExpect(jsonPath("$.code").value("J00"));

        ArgumentCaptor<CreateDiagnosisCatalogCommand> captor =
                ArgumentCaptor.forClass(CreateDiagnosisCatalogCommand.class);
        verify(createUseCase).create(captor.capture());
        assertEquals(" j00 ", captor.getValue().code());
        assertEquals("Respiratory", captor.getValue().diseaseGroup());
    }

    @Test
    void updatesInformationAndStatusWithoutAcceptingCode() throws Exception {
        when(updateUseCase.update(any())).thenReturn(result(true));
        when(updateStatusUseCase.updateStatus(CATALOG_ID, false)).thenReturn(result(false));

        mockMvc.perform(put("/system/diagnosis-catalog/{id}", CATALOG_ID)
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated cold","diseaseGroup":"Respiratory","description":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Common cold"));

        ArgumentCaptor<UpdateDiagnosisCatalogCommand> captor =
                ArgumentCaptor.forClass(UpdateDiagnosisCatalogCommand.class);
        verify(updateUseCase).update(captor.capture());
        assertEquals(CATALOG_ID, captor.getValue().diagnosisCatalogId());

        mockMvc.perform(patch("/system/diagnosis-catalog/{id}/status", CATALOG_ID)
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void exposesDeleteAsTheDeleteGuardOnly() throws Exception {
        org.mockito.Mockito.doThrow(new DiagnosisCatalogDeletionNotAllowedException())
                .when(deleteUseCase).delete(CATALOG_ID);

        mockMvc.perform(delete("/system/diagnosis-catalog/{id}", CATALOG_ID)
                        .with(withManagePermission()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DIAGNOSIS_CATALOG_DELETE_NOT_ALLOWED"));
    }

    @Test
    void returnsStableDiagnosisCatalogBusinessErrorContracts() throws Exception {
        when(queryUseCase.getById(CATALOG_ID)).thenThrow(new DiagnosisCatalogNotFoundException(CATALOG_ID));
        when(createUseCase.create(any())).thenThrow(new DiagnosisCatalogCodeAlreadyExistsException("J00"));
        org.mockito.Mockito.doThrow(new DiagnosisCatalogInUseException(CATALOG_ID))
                .when(deleteUseCase).delete(CATALOG_ID);

        mockMvc.perform(get("/system/diagnosis-catalog/{id}", CATALOG_ID)
                        .with(withManagePermission()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DIAGNOSIS_CATALOG_NOT_FOUND"));

        mockMvc.perform(post("/system/diagnosis-catalog")
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"J00","name":"Common cold","diseaseGroup":"Respiratory"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DIAGNOSIS_CATALOG_CODE_ALREADY_EXISTS"));

        mockMvc.perform(delete("/system/diagnosis-catalog/{id}", CATALOG_ID)
                        .with(withManagePermission()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DIAGNOSIS_CATALOG_IN_USE"));
    }

    @Test
    void rejectsUnauthenticatedAndReadOnlyUsersFromManagementNamespace() throws Exception {
        mockMvc.perform(get("/system/diagnosis-catalog"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/system/diagnosis-catalog")
                        .with(withDoctorReadPermission()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/system/diagnosis-catalog")
                        .with(withDoctorReadPermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"J00","name":"Common cold","diseaseGroup":"Respiratory"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(queryUseCase, createUseCase, updateUseCase, updateStatusUseCase, deleteUseCase);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor withManagePermission() {
        return user("admin").roles("ADMIN")
                .authorities(new SimpleGrantedAuthority("PERMISSION_DIAGNOSIS_CATALOG_MANAGE"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor withDoctorReadPermission() {
        return user("doctor").roles("DOCTOR")
                .authorities(new SimpleGrantedAuthority("PERMISSION_DIAGNOSIS_READ"));
    }

    private DiagnosisCatalogResult result(boolean active) {
        return new DiagnosisCatalogResult(
                CATALOG_ID, "J00", "Common cold", "Respiratory", "Desc", active, CREATED_AT, null
        );
    }
}

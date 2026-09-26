package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
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

import com.benhsoan.adapter.inbound.rest.mapper.SpecialtyRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.specialty.exception.SpecialtyInUseException;
import com.benhsoan.domain.specialty.exception.SpecialtyNameAlreadyExistsException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.specialty.CreateSpecialtyCommand;
import com.benhsoan.port.dto.command.specialty.DeactivateSpecialtyCommand;
import com.benhsoan.port.dto.command.specialty.UpdateSpecialtyCommand;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.dto.result.specialty.SpecialtyDetailResult;
import com.benhsoan.port.inbound.specialty.ActivateSpecialtyUseCase;
import com.benhsoan.port.inbound.specialty.CreateSpecialtyUseCase;
import com.benhsoan.port.inbound.specialty.DeactivateSpecialtyUseCase;
import com.benhsoan.port.inbound.specialty.GetSpecialtyDetailUseCase;
import com.benhsoan.port.inbound.specialty.SearchSpecialtyUseCase;
import com.benhsoan.port.inbound.specialty.UpdateSpecialtyUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = SpecialtyController.class)
@Import({
        AopAutoConfiguration.class,
        SpecialtyControllerTest.AspectTestConfig.class,
        SpecialtyRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class SpecialtyControllerTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SearchSpecialtyUseCase searchSpecialtyUseCase;
    @MockitoBean private GetSpecialtyDetailUseCase getSpecialtyDetailUseCase;
    @MockitoBean private CreateSpecialtyUseCase createSpecialtyUseCase;
    @MockitoBean private UpdateSpecialtyUseCase updateSpecialtyUseCase;
    @MockitoBean private DeactivateSpecialtyUseCase deactivateSpecialtyUseCase;
    @MockitoBean private ActivateSpecialtyUseCase activateSpecialtyUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private static final SimpleGrantedAuthority MANAGE_AUTH = new SimpleGrantedAuthority("PERMISSION_SPECIALTY_MANAGE");
    private static final SimpleGrantedAuthority OTHER_AUTH = new SimpleGrantedAuthority("PERMISSION_PATIENT_READ");

    private final Instant now = Instant.parse("2026-09-18T10:00:00Z");

    private void mockAdminWithSpecialtyManage() {
        UUID adminId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(java.util.Optional.of(
                User.restore(adminId, "admin", "hash", "Admin", "admin@c.com", "0900", roleId, true, now, now)));
        when(roleRepository.findById(roleId)).thenReturn(java.util.Optional.of(
                Role.restore(roleId, "ADMIN", "Admin Role", true, now, now, Set.of("SPECIALTY_MANAGE"))));
    }

    private void mockPatientWithoutSpecialtyManage() {
        UUID patientId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(patientId);
        when(userRepository.findById(patientId)).thenReturn(java.util.Optional.of(
                User.restore(patientId, "patient", "hash", "Patient", "p@c.com", "0900", roleId, true, now, now)));
        when(roleRepository.findById(roleId)).thenReturn(java.util.Optional.of(
                Role.restore(roleId, "PATIENT", "Patient Role", true, now, now, Set.of())));
    }

    @Test
    void listsActiveSpecialtiesForAnyAuthenticatedUser() throws Exception {
        UUID specialtyId = UUID.randomUUID();
        when(searchSpecialtyUseCase.search(null, true)).thenReturn(List.of(
                new SpecialtyResult(specialtyId, "GENERAL", "General", "Khám tổng quát", true)));

        mockMvc.perform(get("/system/specialties").param("active", "true")
                        .with(user("patient").authorities(OTHER_AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(specialtyId.toString()))
                .andExpect(jsonPath("$[0].code").value("GENERAL"))
                .andExpect(jsonPath("$[0].description").value("Khám tổng quát"));
    }

    @Test
    void getsSpecialtyDetailByIdWhenUserHasSpecialtyManagePermission() throws Exception {
        mockAdminWithSpecialtyManage();

        UUID specialtyId = UUID.randomUUID();
        SpecialtyDetailResult detail = SpecialtyDetailResult.builder()
                .id(specialtyId)
                .code("PEDIATRICS")
                .name("Khoa Nhi")
                .description("Chuyên khoa nhi")
                .active(true)
                .doctors(List.of())
                .rooms(List.of())
                .activeTemplateCount(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        when(getSpecialtyDetailUseCase.getById(specialtyId)).thenReturn(detail);

        mockMvc.perform(get("/system/specialties/{id}", specialtyId)
                        .with(user("admin").authorities(MANAGE_AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(specialtyId.toString()))
                .andExpect(jsonPath("$.code").value("PEDIATRICS"))
                .andExpect(jsonPath("$.name").value("Khoa Nhi"))
                .andExpect(jsonPath("$.activeTemplateCount").value(1));
    }

    @Test
    void rejectsGettingSpecialtyDetailWhenUserLacksSpecialtyManagePermission() throws Exception {
        mockPatientWithoutSpecialtyManage();
        UUID specialtyId = UUID.randomUUID();

        mockMvc.perform(get("/system/specialties/{id}", specialtyId)
                        .with(user("patient").authorities(OTHER_AUTH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsSpecialtySuccessfullyWhenUserHasSpecialtyManagePermission() throws Exception {
        mockAdminWithSpecialtyManage();

        UUID specialtyId = UUID.randomUUID();
        SpecialtyDetailResult detail = SpecialtyDetailResult.builder()
                .id(specialtyId)
                .code("PEDIATRICS")
                .name("Khoa Nhi")
                .description("Chuyên khoa nhi")
                .active(true)
                .doctors(List.of())
                .rooms(List.of())
                .activeTemplateCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        when(createSpecialtyUseCase.create(any(CreateSpecialtyCommand.class))).thenReturn(detail);

        mockMvc.perform(post("/system/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "PEDIATRICS",
                                  "name": "Khoa Nhi",
                                  "description": "Chuyên khoa nhi"
                                }
                                """)
                        .with(user("admin").authorities(MANAGE_AUTH)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(specialtyId.toString()))
                .andExpect(jsonPath("$.code").value("PEDIATRICS"));
    }

    @Test
    void updatesSpecialtySuccessfullyWhenUserHasSpecialtyManagePermission() throws Exception {
        mockAdminWithSpecialtyManage();

        UUID specialtyId = UUID.randomUUID();
        SpecialtyDetailResult detail = SpecialtyDetailResult.builder()
                .id(specialtyId)
                .code("PEDIATRICS")
                .name("Khoa Nhi Cập Nhật")
                .description("Mô tả mới")
                .active(true)
                .doctors(List.of())
                .rooms(List.of())
                .activeTemplateCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        when(updateSpecialtyUseCase.update(any(UpdateSpecialtyCommand.class))).thenReturn(detail);

        mockMvc.perform(put("/system/specialties/{id}", specialtyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Khoa Nhi Cập Nhật",
                                  "description": "Mô tả mới",
                                  "doctorIds": [],
                                  "roomIds": []
                                }
                                """)
                        .with(user("admin").authorities(MANAGE_AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(specialtyId.toString()))
                .andExpect(jsonPath("$.name").value("Khoa Nhi Cập Nhật"));
    }

    @Test
    void rejectsCreationWhenUserLacksSpecialtyManagePermission() throws Exception {
        mockPatientWithoutSpecialtyManage();

        mockMvc.perform(post("/system/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "PEDIATRICS",
                                  "name": "Khoa Nhi"
                                }
                                """)
                        .with(user("patient").authorities(OTHER_AUTH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsCreationWhenNameAlreadyExists() throws Exception {
        mockAdminWithSpecialtyManage();
        when(createSpecialtyUseCase.create(any(CreateSpecialtyCommand.class)))
                .thenThrow(new SpecialtyNameAlreadyExistsException("Khoa Nhi"));

        mockMvc.perform(post("/system/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "PEDIATRICS",
                                  "name": "Khoa Nhi"
                                }
                                """)
                        .with(user("admin").authorities(MANAGE_AUTH)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SPECIALTY_NAME_ALREADY_EXISTS"));
    }

    @Test
    void warnsWhenDeactivatingInUseSpecialtyWithoutConfirm() throws Exception {
        mockAdminWithSpecialtyManage();
        UUID specialtyId = UUID.randomUUID();
        when(deactivateSpecialtyUseCase.deactivate(eq(new DeactivateSpecialtyCommand(specialtyId, false))))
                .thenThrow(new SpecialtyInUseException(2L, 1L));

        mockMvc.perform(patch("/system/specialties/{id}/deactivate", specialtyId)
                        .with(user("admin").authorities(MANAGE_AUTH)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SPECIALTY_IN_USE"));
    }

    @Test
    void deactivatesInUseSpecialtyWithConfirm() throws Exception {
        mockAdminWithSpecialtyManage();
        UUID specialtyId = UUID.randomUUID();
        when(deactivateSpecialtyUseCase.deactivate(eq(new DeactivateSpecialtyCommand(specialtyId, true))))
                .thenReturn(new SpecialtyResult(specialtyId, "PEDIATRICS", "Khoa Nhi", false));

        mockMvc.perform(patch("/system/specialties/{id}/deactivate", specialtyId)
                        .param("confirm", "true")
                        .with(user("admin").authorities(MANAGE_AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(specialtyId.toString()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void activatesSpecialtySuccessfully() throws Exception {
        mockAdminWithSpecialtyManage();
        UUID specialtyId = UUID.randomUUID();
        when(activateSpecialtyUseCase.activate(specialtyId))
                .thenReturn(new SpecialtyResult(specialtyId, "PEDIATRICS", "Khoa Nhi", true));

        mockMvc.perform(patch("/system/specialties/{id}/activate", specialtyId)
                        .with(user("admin").authorities(MANAGE_AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(specialtyId.toString()))
                .andExpect(jsonPath("$.active").value(true));
    }
}

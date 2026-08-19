package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.BackupRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.BackupDownloadResult;
import com.benhsoan.port.dto.result.BackupResult;
import com.benhsoan.port.inbound.backup.CreateBackupUseCase;
import com.benhsoan.port.inbound.backup.DownloadBackupUseCase;
import com.benhsoan.port.inbound.backup.GetBackupByIdUseCase;
import com.benhsoan.port.inbound.backup.ListBackupsUseCase;
import com.benhsoan.port.inbound.backup.RestoreBackupUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = BackupController.class)
@Import({BackupRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
        RequirePermissionAspect.class, PermissionEvaluator.class, BackupSecurityIntegrationTest.AspectTestConfig.class})
class BackupSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig { }

    private static final UUID BACKUP_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBackupUseCase createBackupUseCase;
    @MockitoBean
    private ListBackupsUseCase listBackupsUseCase;
    @MockitoBean
    private GetBackupByIdUseCase getBackupByIdUseCase;
    @MockitoBean
    private RestoreBackupUseCase restoreBackupUseCase;
    @MockitoBean
    private DownloadBackupUseCase downloadBackupUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserSessionRepository userSessionRepository;
    @MockitoBean
    private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void allowsAdminToAccessAllBackupEndpoints() throws Exception {
        when(createBackupUseCase.create(any())).thenReturn(backupResult());
        when(listBackupsUseCase.list()).thenReturn(List.of(backupResult()));
        when(getBackupByIdUseCase.getById(any())).thenReturn(backupResult());
        when(restoreBackupUseCase.restore(any())).thenReturn(backupResult());
        when(downloadBackupUseCase.download(any()))
                .thenReturn(new BackupDownloadResult(BACKUP_ID, "file.json", "application/json", new byte[]{1}));

        mockMvc.perform(post("/backups")
                        .with(user("admin").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_BACKUP_CREATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/backups").with(user("admin").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_BACKUP_READ"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/backups/{id}", BACKUP_ID).with(user("admin").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_BACKUP_READ"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/backups/{id}/restore", BACKUP_ID).with(user("admin").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_BACKUP_RESTORE"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/backups/{id}/download", BACKUP_ID).with(user("admin").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_BACKUP_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void forbidsNonAdminRoles() throws Exception {
        for (String role : new String[]{"DOCTOR", "RECEPTIONIST", "MANAGER"}) {
            mockMvc.perform(post("/backups")
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_BACKUP_READ")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/backups").with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_BACKUP_CREATE"))))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/backups/{id}/restore", BACKUP_ID)
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_BACKUP_READ"))))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/backups/{id}/download", BACKUP_ID)
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_BACKUP_CREATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    private BackupResult backupResult() {
        return new BackupResult(
                BACKUP_ID,
                "BKP-20260814-0001",
                "BKP-20260814-0001.json",
                1024L,
                BackupStatus.SUCCESS,
                BackupType.FULL,
                "desc",
                BACKUP_ID,
                Instant.parse("2026-08-14T08:00:00Z"),
                null,
                null
        );
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.BackupRestMapper;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;
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
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = BackupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(BackupRestMapper.class)
class BackupControllerTest {

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

    @Test
    void createsBackup() throws Exception {
        when(createBackupUseCase.create(any())).thenReturn(backupResult());

        mockMvc.perform(post("/backups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backupType\":\"MANUAL\",\"description\":\"nightly\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.backupCode").value("BKP-20260814-0001"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.backupType").value("MANUAL"))
                .andExpect(jsonPath("$.fileName").value("BKP-20260814-0001.json"));
    }

    @Test
    void listsBackups() throws Exception {
        when(listBackupsUseCase.list()).thenReturn(List.of(backupResult()));

        mockMvc.perform(get("/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].backupCode").value("BKP-20260814-0001"));
    }

    @Test
    void getsBackupById() throws Exception {
        when(getBackupByIdUseCase.getById(any(UUID.class))).thenReturn(backupResult());

        mockMvc.perform(get("/backups/{id}", BACKUP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BACKUP_ID.toString()));
    }
    @Test
    void restoresBackup() throws Exception {
        when(restoreBackupUseCase.restore(any(UUID.class))).thenReturn(restoredResult());

        mockMvc.perform(post("/backups/{id}/restore", BACKUP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restoredAt").value("2026-08-14T09:00:00Z"))
                .andExpect(jsonPath("$.restoredBy").value(BACKUP_ID.toString()));
    }

    @Test
    void downloadsBackup() throws Exception {
        when(downloadBackupUseCase.download(any(UUID.class)))
                .thenReturn(new BackupDownloadResult(BACKUP_ID, "BKP-20260814-0001.json", "application/json", new byte[]{1, 2}));

        mockMvc.perform(get("/backups/{id}/download", BACKUP_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"BKP-20260814-0001.json\""));
    }

    private BackupResult backupResult() {
        return new BackupResult(
                BACKUP_ID,
                "BKP-20260814-0001",
                "BKP-20260814-0001.json",
                1024L,
                BackupStatus.SUCCESS,
                BackupType.MANUAL,
                "nightly",
                BACKUP_ID,
                Instant.parse("2026-08-14T08:00:00Z"),
                null,
                null
        );
    }

    private BackupResult restoredResult() {
        return new BackupResult(
                BACKUP_ID,
                "BKP-20260814-0001",
                "BKP-20260814-0001.json",
                1024L,
                BackupStatus.SUCCESS,
                BackupType.MANUAL,
                "nightly",
                BACKUP_ID,
                Instant.parse("2026-08-14T08:00:00Z"),
                Instant.parse("2026-08-14T09:00:00Z"),
                BACKUP_ID
        );
    }
}

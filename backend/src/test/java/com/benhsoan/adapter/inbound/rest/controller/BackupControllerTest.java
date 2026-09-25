package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private com.benhsoan.port.inbound.backup.GetBackupScheduleUseCase getBackupScheduleUseCase;
    @MockitoBean
    private com.benhsoan.port.inbound.backup.UpdateBackupScheduleUseCase updateBackupScheduleUseCase;
    @MockitoBean
    private com.benhsoan.port.inbound.backup.DismissBackupAlertUseCase dismissBackupAlertUseCase;
    @MockitoBean
    private com.benhsoan.port.inbound.backup.VerifyBackupIntegrityUseCase verifyBackupIntegrityUseCase;

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

    @Test
    void getsSchedule() throws Exception {
        com.benhsoan.domain.backup.BackupScheduleConfiguration config =
                com.benhsoan.domain.backup.BackupScheduleConfiguration.createDefault(BACKUP_ID, Instant.parse("2026-08-14T08:00:00Z"));
        when(getBackupScheduleUseCase.getSchedule()).thenReturn(config);

        mockMvc.perform(get("/backups/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyTime").value("02:00"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void updatesSchedule() throws Exception {
        com.benhsoan.domain.backup.BackupScheduleConfiguration config =
                com.benhsoan.domain.backup.BackupScheduleConfiguration.createDefault(BACKUP_ID, Instant.parse("2026-08-14T08:00:00Z"));
        config.updateSchedule(true, "03:30", BACKUP_ID, Instant.parse("2026-08-14T08:00:00Z"));
        when(updateBackupScheduleUseCase.updateSchedule(eq(true), eq("03:30"))).thenReturn(config);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/backups/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"dailyTime\":\"03:30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.dailyTime").value("03:30"));
    }

    @Test
    void updatesScheduleRejectsInvalidDailyTime() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/backups/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"dailyTime\":\"25:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dismissesAlert() throws Exception {
        com.benhsoan.domain.backup.BackupScheduleConfiguration config =
                com.benhsoan.domain.backup.BackupScheduleConfiguration.createDefault(BACKUP_ID, Instant.parse("2026-08-14T08:00:00Z"));
        when(dismissBackupAlertUseCase.dismissAlert()).thenReturn(config);

        mockMvc.perform(post("/backups/schedule/dismiss-alert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertActive").value(false));
    }

    @Test
    void verifiesLatestBackup() throws Exception {
        com.benhsoan.domain.backup.BackupVerificationReport report =
                com.benhsoan.domain.backup.BackupVerificationReport.success(
                        BACKUP_ID,
                        "BKP-20260814-0001",
                        "BKP-20260814-0001.json",
                        28,
                        200,
                        "87",
                        Instant.parse("2026-08-14T08:00:00Z")
                );
        when(verifyBackupIntegrityUseCase.verifyLatest()).thenReturn(report);

        mockMvc.perform(post("/backups/verify-latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.tableCount").value(28))
                .andExpect(jsonPath("$.message").value("Bản sao lưu đọc được và đủ dữ liệu."));
    }

    @Test
    void verifiesBackupById() throws Exception {
        com.benhsoan.domain.backup.BackupVerificationReport report =
                com.benhsoan.domain.backup.BackupVerificationReport.success(
                        BACKUP_ID,
                        "BKP-20260814-0001",
                        "BKP-20260814-0001.json",
                        28,
                        200,
                        "87",
                        Instant.parse("2026-08-14T08:00:00Z")
                );
        when(verifyBackupIntegrityUseCase.verifyById(eq(BACKUP_ID))).thenReturn(report);

        mockMvc.perform(post("/backups/{id}/verify", BACKUP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
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

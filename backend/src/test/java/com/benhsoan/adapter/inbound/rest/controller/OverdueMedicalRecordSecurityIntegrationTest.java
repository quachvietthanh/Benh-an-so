package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDetailRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDiagnosisRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.OverdueMedicalRecordRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.CurrentUserAdapter;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.medicalrecord.GetOverdueMedicalRecordsQuery;
import com.benhsoan.port.dto.command.medicalrecord.SendSigningReminderCommand;
import com.benhsoan.port.dto.result.OverdueMedicalRecordResult;
import com.benhsoan.port.dto.result.SigningReminderResult;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ApplyMedicalRecordTemplateUseCase;
import com.benhsoan.port.inbound.medicalrecord.ArchiveMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.DeleteMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordTemplateSelectionUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordVersionHistoryUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetOverdueMedicalRecordsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetSigningRemindersUseCase;
import com.benhsoan.port.inbound.medicalrecord.IssueMedicalRecordCopyUseCase;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ReplaceMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.SendSigningReminderUseCase;
import com.benhsoan.port.inbound.medicalrecord.SignMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalRecordController.class)
@Import({
        AnonymizationModeState.class,
        AopAutoConfiguration.class,
        OverdueMedicalRecordSecurityIntegrationTest.AspectTestConfig.class,
        MedicalRecordRestMapper.class,
        MedicalRecordDetailRestMapper.class,
        MedicalRecordDiagnosisRestMapper.class,
        OverdueMedicalRecordRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        CurrentUserAdapter.class
})
@DisplayName("Overdue Medical Record Signing & Reminders - Security & API Integration Tests (NCL-11-CN-006)")
class OverdueMedicalRecordSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateMedicalRecordUseCase createMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordUseCase getMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordTemplateSelectionUseCase getMedicalRecordTemplateSelectionUseCase;
    @MockitoBean private ApplyMedicalRecordTemplateUseCase applyMedicalRecordTemplateUseCase;
    @MockitoBean private UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
    @MockitoBean private LockMedicalRecordUseCase lockMedicalRecordUseCase;
    @MockitoBean private SignMedicalRecordUseCase signMedicalRecordUseCase;
    @MockitoBean private AmendMedicalRecordUseCase amendMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordAccessLogsUseCase getMedicalRecordAccessLogsUseCase;
    @MockitoBean private GetMedicalRecordDiagnosesUseCase getMedicalRecordDiagnosesUseCase;
    @MockitoBean private ReplaceMedicalRecordDiagnosesUseCase replaceMedicalRecordDiagnosesUseCase;
    @MockitoBean private ArchiveMedicalRecordUseCase archiveMedicalRecordUseCase;
    @MockitoBean private DeleteMedicalRecordUseCase deleteMedicalRecordUseCase;
    @MockitoBean private IssueMedicalRecordCopyUseCase issueMedicalRecordCopyUseCase;
    @MockitoBean private GetMedicalRecordVersionHistoryUseCase getMedicalRecordVersionHistoryUseCase;
    @MockitoBean private GetOverdueMedicalRecordsUseCase getOverdueMedicalRecordsUseCase;
    @MockitoBean private SendSigningReminderUseCase sendSigningReminderUseCase;
    @MockitoBean private GetSigningRemindersUseCase getSigningRemindersUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private final UUID recordId = UUID.randomUUID();
    private final UUID visitId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-16T12:00:00Z");

    @Test
    @DisplayName("AC-01: Manager/Admin with MEDICAL_RECORD_OVERDUE_READ can list overdue records (returns 200)")
    void managerCanListOverdueMedicalRecords() throws Exception {
        OverdueMedicalRecordResult resultItem = new OverdueMedicalRecordResult(
                recordId,
                MedicalRecordStatus.OPEN,
                visitId,
                "KB-20260914-001",
                now.minus(Duration.ofHours(30)),
                patientId,
                "BN-001",
                "Nguyễn Văn A",
                doctorId,
                "BS Trần B",
                "bs@clinic.vn",
                "0901234567",
                24,
                now.minus(Duration.ofHours(6)),
                6,
                1,
                now.minus(Duration.ofHours(2))
        );

        when(getOverdueMedicalRecordsUseCase.getOverdueRecords(any(GetOverdueMedicalRecordsQuery.class)))
                .thenReturn(new PageImpl<>(List.of(resultItem), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/medical-records/overdue-signing")
                        .with(SecurityMockMvcRequestPostProcessors.user("manager")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_OVERDUE_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].medicalRecordId").value(recordId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].visitCode").value("KB-20260914-001"))
                .andExpect(jsonPath("$.content[0].patientFullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.content[0].doctorFullName").value("BS Trần B"))
                .andExpect(jsonPath("$.content[0].overdueHours").value(6))
                .andExpect(jsonPath("$.content[0].reminderCount").value(1));
    }

    @Test
    @DisplayName("AC-02: Manager/Admin with MEDICAL_RECORD_REMIND_SIGN can send signing reminder (returns 201)")
    void managerCanSendSigningReminder() throws Exception {
        UUID reminderId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        SigningReminderResult result = new SigningReminderResult(
                reminderId,
                recordId,
                doctorId,
                "BS Trần B",
                managerId,
                "Quản lý phòng khám",
                now,
                6,
                "SYSTEM",
                "Vui lòng ký bệnh án sớm",
                "SENT"
        );

        when(sendSigningReminderUseCase.sendReminder(any(SendSigningReminderCommand.class)))
                .thenReturn(result);

        mockMvc.perform(post("/medical-records/{medicalRecordId}/signing-reminders", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"SYSTEM\",\"notes\":\"Vui lòng ký bệnh án sớm\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("manager")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_REMIND_SIGN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reminderId.toString()))
                .andExpect(jsonPath("$.medicalRecordId").value(recordId.toString()))
                .andExpect(jsonPath("$.doctorFullName").value("BS Trần B"))
                .andExpect(jsonPath("$.overdueHours").value(6))
                .andExpect(jsonPath("$.channel").value("SYSTEM"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("AC-03: Signed records are excluded from overdue query (returns empty list)")
    void signedRecordIsExcludedFromOverdueList() throws Exception {
        when(getOverdueMedicalRecordsUseCase.getOverdueRecords(any(GetOverdueMedicalRecordsQuery.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/medical-records/overdue-signing")
                        .with(SecurityMockMvcRequestPostProcessors.user("manager")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_OVERDUE_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("AC-04: Receptionist without permissions is denied (403 Forbidden) and ACCESS_DENIED is logged")
    void receptionistIsForbiddenAndAuditLogged() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        mockMvc.perform(get("/medical-records/overdue-signing")
                        .with(SecurityMockMvcRequestPostProcessors.user("receptionist")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());

        // Verify ACCESS_DENIED audit log was recorded by RequirePermissionAspect
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("AC-04: Receptionist cannot send signing reminder (403 Forbidden)")
    void receptionistCannotSendReminder() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        mockMvc.perform(post("/medical-records/{medicalRecordId}/signing-reminders", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"SYSTEM\",\"notes\":\"Test\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("receptionist")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/medical-records/overdue-signing"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P2: Channel length exceeding 30 characters returns 400 Bad Request")
    void channelLengthExceeding30Returns400() throws Exception {
        String longChannel = "A".repeat(31);
        mockMvc.perform(post("/medical-records/{medicalRecordId}/signing-reminders", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"" + longChannel + "\",\"notes\":\"Test\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("manager")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_REMIND_SIGN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("AC-04: Receptionist cannot view reminders (403 Forbidden)")
    void receptionistCannotViewReminders() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        mockMvc.perform(get("/medical-records/{medicalRecordId}/signing-reminders", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.user("receptionist")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("P1 BOLA: Doctor viewing other doctor reminders returns 403 Forbidden")
    void doctorViewingOtherDoctorRemindersReturns403() throws Exception {
        when(getSigningRemindersUseCase.getReminders(recordId))
                .thenThrow(new com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException());

        mockMvc.perform(get("/medical-records/{medicalRecordId}/signing-reminders", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.user("doctor")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_OVERDUE_READ"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.benhsoan.adapter.inbound.rest.mapper.DoctorScheduleRestMapper;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.result.appointment.AffectedAppointmentResult;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;
import com.benhsoan.port.inbound.appointment.CancelDoctorTimeOffUseCase;
import com.benhsoan.port.inbound.appointment.ConfigureDoctorWeeklyScheduleUseCase;
import com.benhsoan.port.inbound.appointment.GetDoctorScheduleUseCase;
import com.benhsoan.port.inbound.appointment.GetDoctorTimeOffsUseCase;
import com.benhsoan.port.inbound.appointment.RegisterDoctorTimeOffUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = DoctorScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({DoctorScheduleRestMapper.class, GlobalExceptionHandler.class, RequirePermissionAspect.class,
        PermissionEvaluator.class, DoctorScheduleControllerTest.AspectTestConfig.class})
class DoctorScheduleControllerTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectTestConfig {
    }

    private static final UUID DOCTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID TIME_OFF_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01");
    private static final Instant START_TIME = Instant.parse("2099-08-10T08:00:00Z");
    private static final Instant END_TIME = Instant.parse("2099-08-10T12:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ConfigureDoctorWeeklyScheduleUseCase configureDoctorWeeklyScheduleUseCase;
    @MockitoBean private GetDoctorScheduleUseCase getDoctorScheduleUseCase;
    @MockitoBean private RegisterDoctorTimeOffUseCase registerDoctorTimeOffUseCase;
    @MockitoBean private GetDoctorTimeOffsUseCase getDoctorTimeOffsUseCase;
    @MockitoBean private CancelDoctorTimeOffUseCase cancelDoctorTimeOffUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor withPermissions(String... permissions) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "snapshot-user", null, List.of(permissions).stream()
                            .map(permission -> new SimpleGrantedAuthority("PERMISSION_" + permission))
                            .toList()));
            return request;
        };
    }

    @Test
    void getsWeeklyScheduleWithPermission() throws Exception {
        when(getDoctorScheduleUseCase.getWeeklySchedule(DOCTOR_ID)).thenReturn(List.of(
                new DoctorWeeklyScheduleResult(UUID.randomUUID(), DOCTOR_ID, DayOfWeek.MONDAY,
                        LocalTime.of(8, 0), LocalTime.of(12, 0), true)
        ));

        mockMvc.perform(get("/system/doctors/{doctorId}/schedules/weekly", DOCTOR_ID)
                        .with(withPermissions("DOCTOR_SCHEDULE_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].startTime").value("08:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void configuresWeeklyScheduleWithPermission() throws Exception {
        when(configureDoctorWeeklyScheduleUseCase.configureWeeklySchedule(any())).thenReturn(List.of(
                new DoctorWeeklyScheduleResult(UUID.randomUUID(), DOCTOR_ID, DayOfWeek.MONDAY,
                        LocalTime.of(8, 0), LocalTime.of(12, 0), true)
        ));

        mockMvc.perform(put("/system/doctors/{doctorId}/schedules/weekly", DOCTOR_ID)
                        .with(withPermissions("DOCTOR_SCHEDULE_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "schedules": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "startTime": "08:00:00",
                                      "endTime": "12:00:00",
                                      "active": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void registersTimeOffWithPermission() throws Exception {
        when(registerDoctorTimeOffUseCase.registerTimeOff(any())).thenReturn(
                new DoctorTimeOffResult(
                        TIME_OFF_ID, DOCTOR_ID, START_TIME, END_TIME, "Nghi phep",
                        TimeOffStatus.ACTIVE, UUID.randomUUID(), Instant.now(), List.of()
                )
        );

        mockMvc.perform(post("/system/doctors/{doctorId}/time-offs", DOCTOR_ID)
                        .with(withPermissions("DOCTOR_TIMEOFF_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2099-08-10T08:00:00Z",
                                  "endTime": "2099-08-10T12:00:00Z",
                                  "reason": "Nghi phep"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TIME_OFF_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.reason").value("Nghi phep"));
    }

    @Test
    void getsTimeOffsWithPermission() throws Exception {
        UUID apptId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        when(getDoctorTimeOffsUseCase.getTimeOffs(DOCTOR_ID)).thenReturn(List.of(
                new DoctorTimeOffResult(
                        TIME_OFF_ID, DOCTOR_ID, START_TIME, END_TIME, "Nghi phep",
                        TimeOffStatus.ACTIVE, UUID.randomUUID(), Instant.now(),
                        List.of(new AffectedAppointmentResult(
                                apptId, "APT000001", patientId, START_TIME, START_TIME.plusSeconds(1800),
                                AppointmentStatus.SCHEDULED, "Tai kham"
                        ))
                )
        ));

        mockMvc.perform(get("/system/doctors/{doctorId}/time-offs", DOCTOR_ID)
                        .with(withPermissions("DOCTOR_TIMEOFF_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TIME_OFF_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].affectedAppointments[0].id").value(apptId.toString()))
                .andExpect(jsonPath("$[0].affectedAppointments[0].appointmentCode").value("APT000001"));
    }

    @Test
    void cancelsTimeOffWithPermission() throws Exception {
        when(cancelDoctorTimeOffUseCase.cancelTimeOff(DOCTOR_ID, TIME_OFF_ID)).thenReturn(
                new DoctorTimeOffResult(
                        TIME_OFF_ID, DOCTOR_ID, START_TIME, END_TIME, "Nghi phep",
                        TimeOffStatus.CANCELLED, UUID.randomUUID(), Instant.now(), List.of()
                )
        );

        mockMvc.perform(patch("/system/doctors/{doctorId}/time-offs/{timeOffId}/cancel", DOCTOR_ID, TIME_OFF_ID)
                        .with(withPermissions("DOCTOR_TIMEOFF_CANCEL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void tc04_rejectsRequestsWithoutRequiredPermission_returns403AndAuditsAccessDenied() throws Exception {
        // TC-04: Người dùng không có quyền (như Dược sĩ PHARMACIST chỉ có MEDICATION permissions)
        // bị chặn với mã lỗi HTTP 403 Forbidden và hệ thống ghi lại nhật ký cảnh báo vi phạm truy cập (ACCESS_DENIED).
        UUID callerUserId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(callerUserId);

        // 1. Phép gọi POST time-offs nhưng chỉ có quyền PHARMACIST (ví dụ: MEDICATION_READ)
        mockMvc.perform(post("/system/doctors/{doctorId}/time-offs", DOCTOR_ID)
                        .with(withPermissions("MEDICATION_READ"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2099-08-10T08:00:00Z",
                                  "endTime": "2099-08-10T12:00:00Z",
                                  "reason": "Nghi phep"
                                }
                                """))
                .andExpect(status().isForbidden());

        // Kiểm tra audit log ACCESS_DENIED đã được ghi lại
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        org.junit.jupiter.api.Assertions.assertEquals(ResourceType.PERMISSION, audit.getResourceType());
        org.junit.jupiter.api.Assertions.assertEquals(callerUserId, audit.getUserId());

        // 2. Phép gọi PUT weekly schedule nhưng không có quyền DOCTOR_SCHEDULE_UPDATE
        mockMvc.perform(put("/system/doctors/{doctorId}/schedules/weekly", DOCTOR_ID)
                        .with(withPermissions("DOCTOR_SCHEDULE_READ"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "schedules": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "startTime": "08:00:00",
                                      "endTime": "12:00:00",
                                      "active": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}

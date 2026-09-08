package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
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

import com.benhsoan.adapter.inbound.rest.mapper.DoctorScheduleRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.appointment.RegisterDoctorTimeOffCommand;
import com.benhsoan.port.dto.command.appointment.SetDoctorWeeklyScheduleCommand;
import com.benhsoan.port.dto.query.appointment.GetDoctorTimeOffsQuery;
import com.benhsoan.port.dto.query.appointment.GetDoctorWeeklyScheduleQuery;
import com.benhsoan.port.dto.result.appointment.AffectedAppointmentResult;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleItemResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;
import com.benhsoan.port.inbound.appointment.CancelDoctorTimeOffUseCase;
import com.benhsoan.port.inbound.appointment.GetAffectedAppointmentsByTimeOffUseCase;
import com.benhsoan.port.inbound.appointment.GetDoctorTimeOffsUseCase;
import com.benhsoan.port.inbound.appointment.GetDoctorWeeklyScheduleUseCase;
import com.benhsoan.port.inbound.appointment.RegisterDoctorTimeOffUseCase;
import com.benhsoan.port.inbound.appointment.SetDoctorWeeklyScheduleUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = DoctorScheduleController.class)
@Import({
        AopAutoConfiguration.class,
        DoctorScheduleControllerTest.AspectTestConfig.class,
        DoctorScheduleRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class DoctorScheduleControllerTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SetDoctorWeeklyScheduleUseCase setDoctorWeeklyScheduleUseCase;
    @MockitoBean
    private GetDoctorWeeklyScheduleUseCase getDoctorWeeklyScheduleUseCase;
    @MockitoBean
    private RegisterDoctorTimeOffUseCase registerDoctorTimeOffUseCase;
    @MockitoBean
    private GetDoctorTimeOffsUseCase getDoctorTimeOffsUseCase;
    @MockitoBean
    private CancelDoctorTimeOffUseCase cancelDoctorTimeOffUseCase;
    @MockitoBean
    private GetAffectedAppointmentsByTimeOffUseCase getAffectedAppointmentsByTimeOffUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserSessionRepository userSessionRepository;
    @MockitoBean
    private RoleRepository roleRepository;
    @MockitoBean
    private AuditLogRepository auditLogRepository;
    @MockitoBean
    private CurrentUserPort currentUserPort;
    @MockitoBean
    private ClockPort clockPort;

    private static final UUID DOCTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TIME_OFF_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID APPOINTMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ACTOR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void setWeeklySchedule_Authorized_Returns200_TC01() throws Exception {
        DoctorWeeklyScheduleResult result = new DoctorWeeklyScheduleResult(
                DOCTOR_ID,
                List.of(new DoctorWeeklyScheduleItemResult(
                        UUID.randomUUID(), DOCTOR_ID, DayOfWeek.MONDAY,
                        LocalTime.of(8, 0), LocalTime.of(12, 0), true
                ))
        );
        when(setDoctorWeeklyScheduleUseCase.setWeeklySchedule(any(SetDoctorWeeklyScheduleCommand.class)))
                .thenReturn(result);

        String json = """
                {
                  "doctorId": "%s",
                  "items": [
                    {
                      "dayOfWeek": "MONDAY",
                      "startTime": "08:00:00",
                      "endTime": "12:00:00"
                    }
                  ]
                }
                """.formatted(DOCTOR_ID);

        mockMvc.perform(put("/doctor-schedules/weekly")
                        .with(user("manager").authorities(new SimpleGrantedAuthority("PERMISSION_DOCTOR_SCHEDULE_UPDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(DOCTOR_ID.toString()))
                .andExpect(jsonPath("$.items[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.items[0].startTime").value("08:00:00"))
                .andExpect(jsonPath("$.items[0].endTime").value("12:00:00"));

        verify(setDoctorWeeklyScheduleUseCase).setWeeklySchedule(any(SetDoctorWeeklyScheduleCommand.class));
    }

    @Test
    void getWeeklySchedule_Authorized_Returns200() throws Exception {
        DoctorWeeklyScheduleResult result = new DoctorWeeklyScheduleResult(
                DOCTOR_ID,
                List.of(new DoctorWeeklyScheduleItemResult(
                        UUID.randomUUID(), DOCTOR_ID, DayOfWeek.FRIDAY,
                        LocalTime.of(8, 0), LocalTime.of(17, 0), true
                ))
        );
        when(getDoctorWeeklyScheduleUseCase.getWeeklySchedule(any(GetDoctorWeeklyScheduleQuery.class)))
                .thenReturn(result);

        mockMvc.perform(get("/doctor-schedules/weekly")
                        .param("doctorId", DOCTOR_ID.toString())
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_DOCTOR_SCHEDULE_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(DOCTOR_ID.toString()))
                .andExpect(jsonPath("$.items[0].dayOfWeek").value("FRIDAY"));
    }

    @Test
    void registerTimeOff_Returns201WithAffectedAppointments_TC03() throws Exception {
        Instant start = Instant.parse("2026-09-15T08:00:00Z");
        Instant end = Instant.parse("2026-09-15T12:00:00Z");

        AffectedAppointmentResult affected = new AffectedAppointmentResult(
                APPOINTMENT_ID,
                "AP123456",
                UUID.randomUUID(),
                "Nguyen Van A",
                "0901234567",
                start.plusSeconds(1800),
                start.plusSeconds(3600),
                AppointmentStatus.SCHEDULED,
                "Khám định kỳ"
        );

        DoctorTimeOffResult result = new DoctorTimeOffResult(
                TIME_OFF_ID,
                DOCTOR_ID,
                start,
                end,
                "Bác sĩ có việc đột xuất",
                TimeOffStatus.ACTIVE,
                ACTOR_ID,
                start,
                start,
                List.of(affected)
        );

        when(registerDoctorTimeOffUseCase.registerTimeOff(any(RegisterDoctorTimeOffCommand.class)))
                .thenReturn(result);

        String json = """
                {
                  "doctorId": "%s",
                  "startTime": "%s",
                  "endTime": "%s",
                  "reason": "Bác sĩ có việc đột xuất"
                }
                """.formatted(DOCTOR_ID, start, end);

        mockMvc.perform(post("/doctor-time-offs")
                        .with(user("manager").authorities(new SimpleGrantedAuthority("PERMISSION_DOCTOR_TIME_OFF_CREATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TIME_OFF_ID.toString()))
                .andExpect(jsonPath("$.doctorId").value(DOCTOR_ID.toString()))
                .andExpect(jsonPath("$.reason").value("Bác sĩ có việc đột xuất"))
                .andExpect(jsonPath("$.affectedAppointments[0].appointmentId").value(APPOINTMENT_ID.toString()))
                .andExpect(jsonPath("$.affectedAppointments[0].appointmentCode").value("AP123456"))
                .andExpect(jsonPath("$.affectedAppointments[0].patientName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.affectedAppointments[0].patientPhone").value("0901234567"));

        verify(registerDoctorTimeOffUseCase).registerTimeOff(any(RegisterDoctorTimeOffCommand.class));
    }

    @Test
    void pharmacist_AccessDenied_LogsAudit_TC04() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);

        // Pharmacist calls PUT /doctor-schedules/weekly without DOCTOR_SCHEDULE_UPDATE permission
        mockMvc.perform(put("/doctor-schedules/weekly")
                        .with(user("pharmacist").roles("PHARMACIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doctorId": "%s",
                                  "items": []
                                }
                                """.formatted(DOCTOR_ID)))
                .andExpect(status().isForbidden());

        // Verify ACCESS_DENIED audit log was saved
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(ActionType.ACCESS_DENIED, savedAudit.getActionType());

        verifyNoInteractions(setDoctorWeeklyScheduleUseCase);
    }

    @Test
    void cancelTimeOff_Authorized_Returns200() throws Exception {
        Instant now = Instant.parse("2026-09-08T08:00:00Z");
        DoctorTimeOffResult result = new DoctorTimeOffResult(
                TIME_OFF_ID,
                DOCTOR_ID,
                now.plusSeconds(3600),
                now.plusSeconds(7200),
                "Lý do",
                TimeOffStatus.CANCELLED,
                ACTOR_ID,
                now,
                now,
                List.of()
        );
        when(cancelDoctorTimeOffUseCase.cancelTimeOff(TIME_OFF_ID)).thenReturn(result);

        mockMvc.perform(delete("/doctor-time-offs/{id}", TIME_OFF_ID)
                        .with(user("manager").authorities(new SimpleGrantedAuthority("PERMISSION_DOCTOR_TIME_OFF_DELETE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TIME_OFF_ID.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(cancelDoctorTimeOffUseCase).cancelTimeOff(TIME_OFF_ID);
    }

    @Test
    void getAffectedAppointments_Authorized_Returns200() throws Exception {
        AffectedAppointmentResult affected = new AffectedAppointmentResult(
                APPOINTMENT_ID,
                "AP123456",
                UUID.randomUUID(),
                "Nguyen Van A",
                "0901234567",
                Instant.parse("2026-09-15T08:30:00Z"),
                Instant.parse("2026-09-15T09:00:00Z"),
                AppointmentStatus.SCHEDULED,
                "Khám định kỳ"
        );
        when(getAffectedAppointmentsByTimeOffUseCase.getAffectedAppointments(TIME_OFF_ID))
                .thenReturn(List.of(affected));

        mockMvc.perform(get("/doctor-time-offs/{id}/affected-appointments", TIME_OFF_ID)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_DOCTOR_TIME_OFF_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appointmentId").value(APPOINTMENT_ID.toString()))
                .andExpect(jsonPath("$[0].appointmentCode").value("AP123456"))
                .andExpect(jsonPath("$[0].patientName").value("Nguyen Van A"));

        verify(getAffectedAppointmentsByTimeOffUseCase).getAffectedAppointments(TIME_OFF_ID);
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalAppointmentRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.InvalidAppointmentTimeException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.command.appointment.PatientBookAppointmentCommand;
import com.benhsoan.port.dto.query.appointment.GetDoctorAvailableSlotsQuery;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.inbound.appointment.GetDoctorAvailableSlotsUseCase;
import com.benhsoan.port.inbound.appointment.PatientBookAppointmentUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientPortalAppointmentController.class)
@Import({
        PatientPortalAppointmentRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class PatientPortalAppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GetDoctorAvailableSlotsUseCase getDoctorAvailableSlotsUseCase;
    @MockitoBean private PatientBookAppointmentUseCase patientBookAppointmentUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void getAvailableSlotsReturns200() throws Exception {
        UUID doctorId = UUID.randomUUID();

        when(getDoctorAvailableSlotsUseCase.getAvailableSlots(any(GetDoctorAvailableSlotsQuery.class)))
                .thenReturn(List.of(new DoctorAvailableSlotResult(
                        Instant.parse("2099-08-10T02:00:00Z"),
                        Instant.parse("2099-08-10T02:30:00Z"),
                        true)));

        mockMvc.perform(get("/patient-portal/appointments/available-slots")
                        .param("doctorId", doctorId.toString())
                        .param("date", "2099-08-10")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isAvailable").value(true))
                .andExpect(jsonPath("$[0].startTime").exists());
    }

    @Test
    void bookReturns201WithBookingChannel() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        when(patientBookAppointmentUseCase.book(any(PatientBookAppointmentCommand.class)))
                .thenReturn(new PatientAppointmentResult(
                        UUID.randomUUID(), "APT000100", patientId, doctorId,
                        Instant.parse("2099-08-10T02:00:00Z"),
                        Instant.parse("2099-08-10T02:30:00Z"),
                        AppointmentStatus.SCHEDULED,
                        "Đau đầu",
                        "ONLINE_PORTAL",
                        Instant.parse("2026-08-26T02:00:00Z")));

        mockMvc.perform(post("/patient-portal/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doctorId": "%s",
                                  "appointmentDate": "2099-08-10",
                                  "startTime": "09:00",
                                  "reason": "Đau đầu"
                                }
                                """.formatted(doctorId))
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingChannel").value("ONLINE_PORTAL"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.appointmentCode").value("APT000100"));
    }

    @Test
    void bookWithoutDoctorIdReturns400() throws Exception {
        mockMvc.perform(post("/patient-portal/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appointmentDate": "2099-08-10",
                                  "startTime": "09:00"
                                }
                                """)
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void bookWithLongReasonReturns400() throws Exception {
        String longReason = "x".repeat(501);

        mockMvc.perform(post("/patient-portal/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doctorId": "%s",
                                  "appointmentDate": "2099-08-10",
                                  "startTime": "09:00",
                                  "reason": "%s"
                                }
                                """.formatted(UUID.randomUUID(), longReason))
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields.reason").value("Lý do khám không được vượt quá 500 ký tự."));
    }

    @Test
    void bookWithUnalignedSlotReturns400() throws Exception {
        when(patientBookAppointmentUseCase.book(any(PatientBookAppointmentCommand.class)))
                .thenThrow(new InvalidAppointmentTimeException("Khung giờ đặt lịch phải theo mốc 30 phút."));

        mockMvc.perform(post("/patient-portal/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doctorId": "%s",
                                  "appointmentDate": "2099-08-10",
                                  "startTime": "09:17",
                                  "reason": "Đau đầu"
                                }
                                """.formatted(UUID.randomUUID()))
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_TIME_IN_PAST"));
    }
}

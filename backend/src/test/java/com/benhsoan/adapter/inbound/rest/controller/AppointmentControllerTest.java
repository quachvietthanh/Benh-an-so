package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.AppointmentRestMapper;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.result.AppointmentReminderResult;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.benhsoan.port.inbound.appointment.CancelAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.CreateAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.GetAppointmentByIdUseCase;
import com.benhsoan.port.inbound.appointment.GetOverdueAppointmentsUseCase;
import com.benhsoan.port.inbound.appointment.MarkAppointmentNoShowUseCase;
import com.benhsoan.port.inbound.appointment.SearchAppointmentsUseCase;
import com.benhsoan.port.inbound.appointment.SendAppointmentReminderManuallyUseCase;

@WebMvcTest(controllers = AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AppointmentRestMapper.class)
class AppointmentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateAppointmentUseCase createAppointmentUseCase;
    @MockitoBean private CancelAppointmentUseCase cancelAppointmentUseCase;
    @MockitoBean private MarkAppointmentNoShowUseCase markAppointmentNoShowUseCase;
    @MockitoBean private GetOverdueAppointmentsUseCase getOverdueAppointmentsUseCase;
    @MockitoBean private SearchAppointmentsUseCase searchAppointmentsUseCase;
    @MockitoBean private GetAppointmentByIdUseCase getAppointmentByIdUseCase;
    @MockitoBean private SendAppointmentReminderManuallyUseCase sendAppointmentReminderManuallyUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void createsAppointment() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        when(createAppointmentUseCase.create(any())).thenReturn(result(appointmentId, AppointmentStatus.SCHEDULED));

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
                                  "doctorId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
                                  "startTime":"2026-08-10T09:00:00Z",
                                  "endTime":"2026-08-10T09:30:00Z",
                                  "reason":"Tai kham tong quat"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                .andExpect(jsonPath("$.appointmentCode").value("APT000500"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void rejectsCreateWhenReasonIsBlank() throws Exception {
        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
                                  "doctorId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
                                  "startTime":"2026-08-10T09:00:00Z",
                                  "endTime":"2026-08-10T09:30:00Z",
                                  "reason":" "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed."));

        verifyNoInteractions(createAppointmentUseCase);
    }

    @Test
    void getsAppointmentById() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        when(getAppointmentByIdUseCase.getById(appointmentId)).thenReturn(result(appointmentId, AppointmentStatus.SCHEDULED));

        mockMvc.perform(get("/appointments/{id}", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                .andExpect(jsonPath("$.appointmentCode").value("APT000500"));
    }

    @Test
    void searchesAppointments() throws Exception {
        when(searchAppointmentsUseCase.search(any())).thenReturn(new PageImpl<>(
                java.util.List.of(result(UUID.randomUUID(), AppointmentStatus.SCHEDULED))
        ));

        mockMvc.perform(get("/appointments")
                        .param("status", "SCHEDULED")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].appointmentCode").value("APT000500"))
                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"));
    }

    @Test
    void getsOverdueAppointments() throws Exception {
        when(getOverdueAppointmentsUseCase.execute(any())).thenReturn(new PageImpl<>(
                java.util.List.of(result(UUID.randomUUID(), AppointmentStatus.SCHEDULED))
        ));

        mockMvc.perform(get("/appointments/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].appointmentCode").value("APT000500"))
                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"));
    }

    @Test
    void rejectsSearchWhenPageSizeIsInvalid() throws Exception {
        mockMvc.perform(get("/appointments")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Page must be non-negative and size must be between 1 and 100."));

        verifyNoInteractions(searchAppointmentsUseCase);
    }

    @Test
    void cancelsAppointment() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        when(cancelAppointmentUseCase.cancel(any(), any())).thenReturn(result(appointmentId, AppointmentStatus.CANCELLED));

        mockMvc.perform(patch("/appointments/{id}/cancel", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cancelReason":"Patient requested cancellation"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void marksAppointmentNoShow() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        when(markAppointmentNoShowUseCase.execute(any())).thenReturn(result(appointmentId, AppointmentStatus.NO_SHOW));

        mockMvc.perform(patch("/appointments/{id}/no-show", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));
    }

    @Test
    void sendsAppointmentReminder() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        when(sendAppointmentReminderManuallyUseCase.sendManually(appointmentId))
                .thenReturn(AppointmentReminderResult.sent());

        mockMvc.perform(post("/appointments/{id}/reminder", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.message").value("Appointment reminder was sent."));
    }

    private AppointmentResult result(UUID appointmentId, AppointmentStatus status) {
        return new AppointmentResult(
                appointmentId,
                "APT000500",
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001"),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"),
                Instant.parse("2026-08-10T09:00:00Z"),
                Instant.parse("2026-08-10T09:30:00Z"),
                status,
                "Tai kham tong quat",
                status == AppointmentStatus.CANCELLED ? "Patient requested cancellation" : null,
                null,
                null,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5"),
                Instant.parse("2026-08-09T02:00:00Z")
        );
    }
}

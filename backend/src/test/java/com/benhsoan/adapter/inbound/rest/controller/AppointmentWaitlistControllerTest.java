package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import com.benhsoan.adapter.inbound.rest.mapper.AppointmentWaitlistRestMapper;
import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;
import com.benhsoan.port.dto.result.appointment.WaitlistSuggestionResult;
import com.benhsoan.port.inbound.appointment.AddToWaitlistUseCase;
import com.benhsoan.port.inbound.appointment.CancelWaitlistEntryUseCase;
import com.benhsoan.port.inbound.appointment.GetAppointmentWaitlistUseCase;
import com.benhsoan.port.inbound.appointment.GetWaitlistSuggestionUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = AppointmentWaitlistController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        AppointmentWaitlistRestMapper.class,
        GlobalExceptionHandler.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        AppointmentWaitlistControllerTest.AspectTestConfig.class
})
class AppointmentWaitlistControllerTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddToWaitlistUseCase addToWaitlistUseCase;

    @MockitoBean
    private GetAppointmentWaitlistUseCase getAppointmentWaitlistUseCase;

    @MockitoBean
    private GetWaitlistSuggestionUseCase getWaitlistSuggestionUseCase;

    @MockitoBean
    private CancelWaitlistEntryUseCase cancelWaitlistEntryUseCase;

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

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor withPermissions(String... permissions) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "test-user",
                            null,
                            List.of(permissions).stream()
                                    .map(p -> new SimpleGrantedAuthority("PERMISSION_" + p))
                                    .toList()
                    )
            );
            return request;
        };
    }

    @Test
    @DisplayName("POST /appointments/waitlist trả về 201 Created khi thêm thành công")
    void shouldReturn201WhenAddToWaitlistSucceeds() throws Exception {
        UUID waitlistId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDate desiredDate = LocalDate.of(2026, 10, 20);

        AppointmentWaitlistResult result = AppointmentWaitlistResult.builder()
                .id(waitlistId)
                .patientId(patientId)
                .patientName("Nguyễn Văn A")
                .doctorId(doctorId)
                .doctorName("BS. Trần B")
                .desiredDate(desiredDate)
                .timePreference(TimePreference.ANYTIME)
                .status(WaitlistStatus.WAITING)
                .note("Khám nội")
                .createdAt(Instant.parse("2026-10-01T08:00:00Z"))
                .build();

        when(addToWaitlistUseCase.addToWaitlist(any())).thenReturn(result);

        String json = """
                {
                  "patientId": "%s",
                  "doctorId": "%s",
                  "desiredDate": "%s",
                  "timePreference": "ANYTIME",
                  "note": "Khám nội"
                }
                """.formatted(patientId, doctorId, desiredDate);

        mockMvc.perform(post("/appointments/waitlist")
                        .with(withPermissions("APPOINTMENT_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(waitlistId.toString()))
                .andExpect(jsonPath("$.patientName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("POST /appointments/waitlist trả về 400 Bad Request khi thiếu trường bắt buộc")
    void shouldReturn400WhenRequiredFieldMissing() throws Exception {
        String json = """
                {
                  "patientId": null,
                  "doctorId": null,
                  "desiredDate": null
                }
                """;

        mockMvc.perform(post("/appointments/waitlist")
                        .with(withPermissions("APPOINTMENT_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /appointments/waitlist trả về 200 OK và danh sách chờ")
    void shouldReturn200AndWaitlist() throws Exception {
        AppointmentWaitlistResult result = AppointmentWaitlistResult.builder()
                .id(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .patientName("Bệnh nhân 1")
                .doctorId(UUID.randomUUID())
                .desiredDate(LocalDate.of(2026, 10, 20))
                .timePreference(TimePreference.MORNING)
                .status(WaitlistStatus.WAITING)
                .createdAt(Instant.parse("2026-10-01T08:00:00Z"))
                .build();

        when(getAppointmentWaitlistUseCase.getWaitlist(any())).thenReturn(List.of(result));

        mockMvc.perform(get("/appointments/waitlist")
                        .with(withPermissions("APPOINTMENT_READ"))
                        .param("status", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientName").value("Bệnh nhân 1"))
                .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    @DisplayName("GET /appointments/waitlist/suggest trả về 200 OK khi có gợi ý")
    void shouldReturn200WhenSuggestionFound() throws Exception {
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 20);

        WaitlistSuggestionResult suggestion = WaitlistSuggestionResult.builder()
                .waitlistId(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .patientName("Nguyễn Văn A")
                .doctorId(doctorId)
                .desiredDate(date)
                .timePreference(TimePreference.ANYTIME)
                .createdAt(Instant.parse("2026-10-01T08:00:00Z"))
                .build();

        when(getWaitlistSuggestionUseCase.getSuggestion(any())).thenReturn(Optional.of(suggestion));

        mockMvc.perform(get("/appointments/waitlist/suggest")
                        .with(withPermissions("APPOINTMENT_READ"))
                        .param("doctorId", doctorId.toString())
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Nguyễn Văn A"));
    }

    @Test
    @DisplayName("GET /appointments/waitlist/suggest trả về 204 No Content khi không có người chờ")
    void shouldReturn204WhenNoSuggestionFound() throws Exception {
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 20);

        when(getWaitlistSuggestionUseCase.getSuggestion(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/appointments/waitlist/suggest")
                        .with(withPermissions("APPOINTMENT_READ"))
                        .param("doctorId", doctorId.toString())
                        .param("date", date.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /appointments/waitlist/{id}/cancel trả về 200 OK khi hủy mục chờ")
    void shouldReturn200WhenCancelSucceeds() throws Exception {
        UUID waitlistId = UUID.randomUUID();

        AppointmentWaitlistResult result = AppointmentWaitlistResult.builder()
                .id(waitlistId)
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .desiredDate(LocalDate.of(2026, 10, 20))
                .status(WaitlistStatus.CANCELLED)
                .cancelReason("Bệnh nhân chuyển lịch")
                .build();

        when(cancelWaitlistEntryUseCase.cancel(any())).thenReturn(result);

        String json = """
                {
                  "reason": "Bệnh nhân chuyển lịch"
                }
                """;

        mockMvc.perform(patch("/appointments/waitlist/" + waitlistId + "/cancel")
                        .with(withPermissions("APPOINTMENT_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Bệnh nhân chuyển lịch"));
    }
}

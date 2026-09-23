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
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.benhsoan.adapter.inbound.rest.mapper.AppointmentRestMapper;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.result.AppointmentReminderResult;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.benhsoan.port.inbound.appointment.CancelAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.ConfirmAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.CreateAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.GetAppointmentByIdUseCase;
import com.benhsoan.port.inbound.appointment.GetOverdueAppointmentsUseCase;
import com.benhsoan.port.inbound.appointment.GetUnconfirmedAppointmentsUseCase;
import com.benhsoan.port.inbound.appointment.MarkAppointmentNoShowUseCase;
import com.benhsoan.port.inbound.appointment.RescheduleAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.SearchAppointmentsUseCase;
import com.benhsoan.port.inbound.appointment.SendAppointmentReminderManuallyUseCase;
import com.benhsoan.port.dto.result.appointment.AppointmentRescheduleHistoryResult;

import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;

@WebMvcTest(controllers = AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ AppointmentRestMapper.class, com.benhsoan.adapter.inbound.rest.mapper.AppointmentSeriesRestMapper.class, AnonymizationModeState.class, GlobalExceptionHandler.class,
                RequirePermissionAspect.class,
                PermissionEvaluator.class, AppointmentControllerTest.AspectTestConfig.class })
class AppointmentControllerTest {

        @TestConfiguration(proxyBeanMethods = false)
        @EnableAspectJAutoProxy
        static class AspectTestConfig {
        }

        private static final Instant APPOINTMENT_START = Instant.parse("2099-08-10T09:00:00Z");
        private static final Instant APPOINTMENT_END = Instant.parse("2099-08-10T09:30:00Z");

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private CreateAppointmentUseCase createAppointmentUseCase;
        @MockitoBean
        private CancelAppointmentUseCase cancelAppointmentUseCase;
        @MockitoBean
        private ConfirmAppointmentUseCase confirmAppointmentUseCase;
        @MockitoBean
        private MarkAppointmentNoShowUseCase markAppointmentNoShowUseCase;
        @MockitoBean
        private GetOverdueAppointmentsUseCase getOverdueAppointmentsUseCase;
        @MockitoBean
        private GetUnconfirmedAppointmentsUseCase getUnconfirmedAppointmentsUseCase;
        @MockitoBean
        private SearchAppointmentsUseCase searchAppointmentsUseCase;
        @MockitoBean
        private GetAppointmentByIdUseCase getAppointmentByIdUseCase;
        @MockitoBean
        private RescheduleAppointmentUseCase rescheduleAppointmentUseCase;
        @MockitoBean
        private SendAppointmentReminderManuallyUseCase sendAppointmentReminderManuallyUseCase;
        @MockitoBean
        private com.benhsoan.port.inbound.appointment.GetDoctorWeeklyScheduleTableUseCase getDoctorWeeklyScheduleTableUseCase;
        @MockitoBean
        private com.benhsoan.port.inbound.appointment.PreviewAppointmentSeriesUseCase previewAppointmentSeriesUseCase;
        @MockitoBean
        private com.benhsoan.port.inbound.appointment.CreateAppointmentSeriesUseCase createAppointmentSeriesUseCase;
        @MockitoBean
        private com.benhsoan.port.inbound.appointment.GetAppointmentSeriesByIdUseCase getAppointmentSeriesByIdUseCase;
        @MockitoBean
        private com.benhsoan.port.inbound.appointment.GetPatientAppointmentSeriesUseCase getPatientAppointmentSeriesUseCase;
        @MockitoBean
        private JwtTokenPort jwtTokenPort;
        @MockitoBean
        private UserRepository userRepository;
        @MockitoBean
        private UserSessionRepository userSessionRepository;
        @MockitoBean
        private CurrentUserPort currentUserPort;
        @MockitoBean
        private ClockPort clockPort;
        @MockitoBean
        private RoleRepository roleRepository;
        @MockitoBean
        private AuditLogRepository auditLogRepository;

        @org.junit.jupiter.api.AfterEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void createsAppointment() throws Exception {
                UUID appointmentId = UUID.randomUUID();
                when(createAppointmentUseCase.create(any()))
                                .thenReturn(result(appointmentId, AppointmentStatus.SCHEDULED));

                mockMvc.perform(post("/appointments")
                                .with(withPermissions("APPOINTMENT_CREATE"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "patientId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
                                                  "doctorId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
                                                  "startTime":"2099-08-10T09:00:00Z",
                                                  "endTime":"2099-08-10T09:30:00Z",
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
                                .with(withPermissions("APPOINTMENT_CREATE"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "patientId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
                                                  "doctorId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
                                                  "startTime":"2099-08-10T09:00:00Z",
                                                  "endTime":"2099-08-10T09:30:00Z",
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
                when(getAppointmentByIdUseCase.getById(appointmentId))
                                .thenReturn(result(appointmentId, AppointmentStatus.SCHEDULED));

                mockMvc.perform(get("/appointments/{id}", appointmentId).with(withPermissions("APPOINTMENT_READ")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                                .andExpect(jsonPath("$.appointmentCode").value("APT000500"));
        }

        @Test
        void searchesAppointments() throws Exception {
                when(searchAppointmentsUseCase.search(any())).thenReturn(new PageImpl<>(
                                java.util.List.of(result(UUID.randomUUID(), AppointmentStatus.SCHEDULED))));

                mockMvc.perform(get("/appointments")
                                .with(withPermissions("APPOINTMENT_READ"))
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
                                java.util.List.of(result(UUID.randomUUID(), AppointmentStatus.SCHEDULED))));

                mockMvc.perform(get("/appointments/overdue").with(withPermissions("APPOINTMENT_READ")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].appointmentCode").value("APT000500"))
                                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"));
        }

        @Test
        void rejectsSearchWhenPageSizeIsInvalid() throws Exception {
                mockMvc.perform(get("/appointments")
                                .with(withPermissions("APPOINTMENT_READ"))
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
                when(cancelAppointmentUseCase.cancel(any(), any()))
                                .thenReturn(result(appointmentId, AppointmentStatus.CANCELLED));

                mockMvc.perform(patch("/appointments/{id}/cancel", appointmentId)
                                .with(withPermissions("APPOINTMENT_UPDATE"))
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
                when(markAppointmentNoShowUseCase.execute(any()))
                                .thenReturn(result(appointmentId, AppointmentStatus.NO_SHOW));

                mockMvc.perform(patch("/appointments/{id}/no-show", appointmentId)
                                .with(withPermissions("APPOINTMENT_UPDATE")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("NO_SHOW"));
        }

        @Test
        void sendsAppointmentReminder() throws Exception {
                UUID appointmentId = UUID.randomUUID();
                when(sendAppointmentReminderManuallyUseCase.sendManually(appointmentId))
                                .thenReturn(AppointmentReminderResult.sent());

                mockMvc.perform(post("/appointments/{id}/reminder", appointmentId)
                                .with(withPermissions("APPOINTMENT_UPDATE")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SENT"))
                                .andExpect(jsonPath("$.message").value("Appointment reminder was sent."));
        }

        @Test
        void reschedulesAppointmentSuccessfully() throws Exception {
                UUID appointmentId = UUID.randomUUID();
                UUID newDoctorId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");
                Instant newStart = Instant.parse("2099-08-11T10:00:00Z");
                Instant newEnd = Instant.parse("2099-08-11T10:30:00Z");

                AppointmentRescheduleHistoryResult historyResult = AppointmentRescheduleHistoryResult.builder()
                                .id(UUID.randomUUID())
                                .appointmentId(appointmentId)
                                .oldDoctorId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"))
                                .newDoctorId(newDoctorId)
                                .oldDoctorName("Dr. Old")
                                .newDoctorName("Dr. New")
                                .oldStartTime(APPOINTMENT_START)
                                .oldEndTime(APPOINTMENT_END)
                                .newStartTime(newStart)
                                .newEndTime(newEnd)
                                .reason("Benh nhan doi gio")
                                .rescheduledBy(UUID.randomUUID())
                                .rescheduledByName("Receptionist")
                                .rescheduledAt(Instant.parse("2026-08-09T03:00:00Z"))
                                .build();

                AppointmentResult updatedResult = new AppointmentResult(
                                appointmentId,
                                "APT000500",
                                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001"),
                                newDoctorId,
                                newStart,
                                newEnd,
                                AppointmentStatus.SCHEDULED,
                                "Tai kham tong quat",
                                null,
                                null,
                                null,
                                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5"),
                                Instant.parse("2026-08-09T02:00:00Z"),
                                List.of(historyResult));

                when(rescheduleAppointmentUseCase.reschedule(any(), any())).thenReturn(updatedResult);

                mockMvc.perform(patch("/appointments/{id}/reschedule", appointmentId)
                                .with(withPermissions("APPOINTMENT_UPDATE"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "newDoctorId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3",
                                                  "startTime":"2099-08-11T10:00:00Z",
                                                  "endTime":"2099-08-11T10:30:00Z",
                                                  "reason":"Benh nhan doi gio"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                                .andExpect(jsonPath("$.doctorId").value(newDoctorId.toString()))
                                .andExpect(jsonPath("$.startTime").value("2099-08-11T10:00:00Z"))
                                .andExpect(jsonPath("$.endTime").value("2099-08-11T10:30:00Z"))
                                .andExpect(jsonPath("$.rescheduleHistories[0].reason").value("Benh nhan doi gio"))
                                .andExpect(jsonPath("$.rescheduleHistories[0].oldDoctorName").value("Dr. Old"))
                                .andExpect(jsonPath("$.rescheduleHistories[0].newDoctorName").value("Dr. New"));
        }

        @Test
        void rejectsRescheduleWhenInvalid() throws Exception {
                UUID appointmentId = UUID.randomUUID();

                mockMvc.perform(patch("/appointments/{id}/reschedule", appointmentId)
                                .with(withPermissions("APPOINTMENT_UPDATE"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "newDoctorId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3",
                                                  "startTime":null,
                                                  "endTime":"2099-08-11T10:30:00Z",
                                                  "reason":""
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed."));

                verifyNoInteractions(rescheduleAppointmentUseCase);
        }

        @Test
        void rejectsRequestsWithoutTheRequiredPermission() throws Exception {
                mockMvc.perform(get("/appointments").with(withPermissions("APPOINTMENT_UPDATE")))
                                .andExpect(status().isForbidden());
                mockMvc.perform(post("/appointments")
                                .with(withPermissions("APPOINTMENT_READ"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequest()))
                                .andExpect(status().isForbidden());
                mockMvc.perform(patch("/appointments/{id}/cancel", UUID.randomUUID())
                                .with(withPermissions("APPOINTMENT_READ"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cancelReason\":\"Patient requested cancellation\"}"))
                                .andExpect(status().isForbidden());
                mockMvc.perform(patch("/appointments/{id}/reschedule", UUID.randomUUID())
                                .with(withPermissions("APPOINTMENT_READ"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"startTime\":\"2099-08-11T10:00:00Z\",\"endTime\":\"2099-08-11T10:30:00Z\",\"reason\":\"Doi gio\"}"))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(searchAppointmentsUseCase, createAppointmentUseCase, cancelAppointmentUseCase,
                                rescheduleAppointmentUseCase);
        }

        private RequestPostProcessor withPermissions(String... permissions) {
                return request -> {
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                                        "snapshot-user", null, List.of(permissions).stream()
                                                        .map(permission -> new SimpleGrantedAuthority(
                                                                        "PERMISSION_" + permission))
                                                        .toList()));
                        return request;
                };
        }

        private static String createRequest() {
                return """
                                {"patientId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001","doctorId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2","startTime":"2099-08-10T09:00:00Z","endTime":"2099-08-10T09:30:00Z","reason":"Tai kham tong quat"}
                                """;
        }

        private AppointmentResult result(UUID appointmentId, AppointmentStatus status) {
                return new AppointmentResult(
                                appointmentId,
                                "APT000500",
                                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001"),
                                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"),
                                APPOINTMENT_START,
                                APPOINTMENT_END,
                                status,
                                "Tai kham tong quat",
                                status == AppointmentStatus.CANCELLED ? "Patient requested cancellation" : null,
                                null,
                                null,
                                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5"),
                                Instant.parse("2026-08-09T02:00:00Z"),
                                status == AppointmentStatus.CONFIRMED ? Instant.parse("2026-08-09T03:00:00Z") : null,
                                status == AppointmentStatus.CONFIRMED
                                                ? UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5")
                                                : null,
                                status == AppointmentStatus.CONFIRMED ? "Lễ Tân Nguyễn Văn A" : null);
        }

        @Test
        void confirmAppointment_returnsConfirmedResponse() throws Exception {
                UUID appointmentId = UUID.randomUUID();
                when(confirmAppointmentUseCase.confirm(appointmentId))
                                .thenReturn(result(appointmentId, AppointmentStatus.CONFIRMED));

                mockMvc.perform(patch("/appointments/{id}/confirm", appointmentId)
                                .with(withPermissions("APPOINTMENT_UPDATE")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                                .andExpect(jsonPath("$.confirmedByName").value("Lễ Tân Nguyễn Văn A"));
        }

        @Test
        void confirmAppointment_withoutPermission_forbidden() throws Exception {
                UUID appointmentId = UUID.randomUUID();

                mockMvc.perform(patch("/appointments/{id}/confirm", appointmentId)
                                .with(withPermissions("APPOINTMENT_READ")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void getUnconfirmedAppointments_returnsPagedResponse() throws Exception {
                UUID appointmentId = UUID.randomUUID();
                when(getUnconfirmedAppointmentsUseCase.getUnconfirmed(any(), any()))
                                .thenReturn(new PageImpl<>(
                                                List.of(result(appointmentId, AppointmentStatus.SCHEDULED))));

                mockMvc.perform(get("/appointments/unconfirmed")
                                .param("date", "2026-09-14")
                                .with(withPermissions("APPOINTMENT_READ")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].id").value(appointmentId.toString()))
                                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"));
        }

        @Test
        void previewSeries_returnsPreviewResponse() throws Exception {
                UUID patientId = UUID.randomUUID();
                UUID doctorId = UUID.randomUUID();

                when(previewAppointmentSeriesUseCase.preview(any()))
                                .thenReturn(com.benhsoan.port.dto.result.appointment.AppointmentSeriesPreviewResult.builder()
                                                .totalSessions(2)
                                                .intervalDays(7)
                                                .allAvailable(true)
                                                .conflictCount(0)
                                                .sessions(List.of(
                                                                com.benhsoan.port.dto.result.appointment.AppointmentSeriesSessionPreviewResult.builder()
                                                                                .sequenceNumber(1)
                                                                                .startTime(APPOINTMENT_START)
                                                                                .endTime(APPOINTMENT_END)
                                                                                .status("AVAILABLE")
                                                                                .build()
                                                ))
                                                .build());

                String payload = """
                                {
                                  "patientId": "%s",
                                  "doctorId": "%s",
                                  "firstSessionStartTime": "%s",
                                  "sessionDurationMinutes": 30,
                                  "totalSessions": 2,
                                  "intervalDays": 7
                                }
                                """.formatted(patientId, doctorId, APPOINTMENT_START);

                mockMvc.perform(post("/appointments/series/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                                .with(withPermissions("APPOINTMENT_CREATE")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalSessions").value(2))
                                .andExpect(jsonPath("$.allAvailable").value(true))
                                .andExpect(jsonPath("$.sessions[0].sequenceNumber").value(1));
        }

        @Test
        void createSeries_returnsCreatedResponse() throws Exception {
                UUID seriesId = UUID.randomUUID();
                UUID patientId = UUID.randomUUID();
                UUID doctorId = UUID.randomUUID();

                when(createAppointmentSeriesUseCase.create(any()))
                                .thenReturn(com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult.builder()
                                                .id(seriesId)
                                                .seriesCode("SER000001")
                                                .patientId(patientId)
                                                .doctorId(doctorId)
                                                .totalSessions(2)
                                                .intervalDays(7)
                                                .title("Lieu trinh VLTL")
                                                .status(com.benhsoan.domain.appointment.enums.AppointmentSeriesStatus.ACTIVE)
                                                .appointments(List.of())
                                                .build());

                String payload = """
                                {
                                  "patientId": "%s",
                                  "doctorId": "%s",
                                  "title": "Lieu trinh VLTL",
                                  "totalSessions": 2,
                                  "intervalDays": 7,
                                  "sessions": [
                                    {
                                      "sequenceNumber": 1,
                                      "startTime": "%s",
                                      "endTime": "%s"
                                    },
                                    {
                                      "sequenceNumber": 2,
                                      "startTime": "%s",
                                      "endTime": "%s"
                                    }
                                  ]
                                }
                                """.formatted(patientId, doctorId, APPOINTMENT_START, APPOINTMENT_END,
                                                APPOINTMENT_START.plusSeconds(86400 * 7), APPOINTMENT_END.plusSeconds(86400 * 7));

                mockMvc.perform(post("/appointments/series")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                                .with(withPermissions("APPOINTMENT_CREATE")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(seriesId.toString()))
                                .andExpect(jsonPath("$.seriesCode").value("SER000001"))
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void createSeries_whenConflict_returns409WithDetails() throws Exception {
                UUID patientId = UUID.randomUUID();
                UUID doctorId = UUID.randomUUID();

                var conflictDetail = new com.benhsoan.domain.appointment.AppointmentSeriesConflictDetail(
                                2, APPOINTMENT_START.plusSeconds(86400 * 7), APPOINTMENT_END.plusSeconds(86400 * 7),
                                "APPOINTMENT_CONFLICT", "Bác sĩ đã có lịch hẹn khác trong khung giờ này."
                );

                when(createAppointmentSeriesUseCase.create(any()))
                                .thenThrow(new com.benhsoan.domain.appointment.exception.AppointmentSeriesConflictException(List.of(conflictDetail)));

                String payload = """
                                {
                                  "patientId": "%s",
                                  "doctorId": "%s",
                                  "totalSessions": 2,
                                  "intervalDays": 7,
                                  "sessions": [
                                    {
                                      "sequenceNumber": 1,
                                      "startTime": "%s",
                                      "endTime": "%s"
                                    },
                                    {
                                      "sequenceNumber": 2,
                                      "startTime": "%s",
                                      "endTime": "%s"
                                    }
                                  ]
                                }
                                """.formatted(patientId, doctorId, APPOINTMENT_START, APPOINTMENT_END,
                                                APPOINTMENT_START.plusSeconds(86400 * 7), APPOINTMENT_END.plusSeconds(86400 * 7));

                mockMvc.perform(post("/appointments/series")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                                .with(withPermissions("APPOINTMENT_CREATE")))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.code").value("APPOINTMENT_SERIES_CONFLICT"))
                                .andExpect(jsonPath("$.details.conflicts[0].sequenceNumber").value(2))
                                .andExpect(jsonPath("$.details.conflicts[0].conflictType").value("APPOINTMENT_CONFLICT"));
        }

        @Test
        void getSeriesById_returnsSeries() throws Exception {
                UUID seriesId = UUID.randomUUID();
                when(getAppointmentSeriesByIdUseCase.getById(seriesId))
                                .thenReturn(com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult.builder()
                                                .id(seriesId)
                                                .seriesCode("SER000001")
                                                .status(com.benhsoan.domain.appointment.enums.AppointmentSeriesStatus.ACTIVE)
                                                .appointments(List.of())
                                                .build());

                mockMvc.perform(get("/appointments/series/{id}", seriesId)
                                .with(withPermissions("APPOINTMENT_READ")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(seriesId.toString()))
                                .andExpect(jsonPath("$.seriesCode").value("SER000001"));
        }

        @Test
        void getSeriesByPatientId_returnsList() throws Exception {
                UUID patientId = UUID.randomUUID();
                UUID seriesId = UUID.randomUUID();

                when(getPatientAppointmentSeriesUseCase.getByPatientId(patientId))
                                .thenReturn(List.of(
                                                com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult.builder()
                                                                .id(seriesId)
                                                                .seriesCode("SER000001")
                                                                .patientId(patientId)
                                                                .status(com.benhsoan.domain.appointment.enums.AppointmentSeriesStatus.ACTIVE)
                                                                .appointments(List.of())
                                                                .build()
                                ));

                mockMvc.perform(get("/appointments/series/patient/{patientId}", patientId)
                                .with(withPermissions("APPOINTMENT_READ")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(seriesId.toString()))
                                .andExpect(jsonPath("$[0].seriesCode").value("SER000001"));
        }

        @Test
        void previewSeries_whenMissingAppointmentCreatePermission_returns403() throws Exception {
                String payload = """
                                {
                                  "patientId": "%s",
                                  "doctorId": "%s",
                                  "firstSessionStartTime": "%s",
                                  "sessionDurationMinutes": 30,
                                  "totalSessions": 2,
                                  "intervalDays": 7
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(), APPOINTMENT_START);

                mockMvc.perform(post("/appointments/series/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                                .with(withPermissions("APPOINTMENT_READ")))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(previewAppointmentSeriesUseCase);
        }

        @Test
        void previewSeries_whenUnauthorizedRole_returns403() throws Exception {
                when(previewAppointmentSeriesUseCase.preview(any()))
                                .thenThrow(new com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException());

                String payload = """
                                {
                                  "patientId": "%s",
                                  "doctorId": "%s",
                                  "firstSessionStartTime": "%s",
                                  "sessionDurationMinutes": 30,
                                  "totalSessions": 2,
                                  "intervalDays": 7
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(), APPOINTMENT_START);

                mockMvc.perform(post("/appointments/series/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                                .with(withPermissions("APPOINTMENT_CREATE")))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_APPOINTMENT_OPERATION"));
        }

        @Test
        void createSeries_whenMissingAppointmentCreatePermission_returns403() throws Exception {
                String payload = """
                                {
                                  "patientId": "%s",
                                  "doctorId": "%s",
                                  "totalSessions": 2,
                                  "intervalDays": 7,
                                  "sessions": [
                                    {
                                      "sequenceNumber": 1,
                                      "startTime": "%s",
                                      "endTime": "%s"
                                    },
                                    {
                                      "sequenceNumber": 2,
                                      "startTime": "%s",
                                      "endTime": "%s"
                                    }
                                  ]
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(), APPOINTMENT_START, APPOINTMENT_END,
                                                APPOINTMENT_START.plusSeconds(86400 * 7), APPOINTMENT_END.plusSeconds(86400 * 7));

                mockMvc.perform(post("/appointments/series")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                                .with(withPermissions("APPOINTMENT_READ")))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(createAppointmentSeriesUseCase);
        }

        @Test
        void createSeries_whenUnauthorizedRole_returns403() throws Exception {
                when(createAppointmentSeriesUseCase.create(any()))
                                .thenThrow(new com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException());

                String payload = """
                                {
                                  "patientId": "%s",
                                  "doctorId": "%s",
                                  "totalSessions": 2,
                                  "intervalDays": 7,
                                  "sessions": [
                                    {
                                      "sequenceNumber": 1,
                                      "startTime": "%s",
                                      "endTime": "%s"
                                    },
                                    {
                                      "sequenceNumber": 2,
                                      "startTime": "%s",
                                      "endTime": "%s"
                                    }
                                  ]
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(), APPOINTMENT_START, APPOINTMENT_END,
                                                APPOINTMENT_START.plusSeconds(86400 * 7), APPOINTMENT_END.plusSeconds(86400 * 7));

                mockMvc.perform(post("/appointments/series")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                                .with(withPermissions("APPOINTMENT_CREATE")))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_APPOINTMENT_OPERATION"));
        }
}

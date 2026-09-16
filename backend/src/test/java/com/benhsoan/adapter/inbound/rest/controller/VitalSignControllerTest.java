package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

import com.benhsoan.adapter.inbound.rest.mapper.VitalSignRestMapper;
import com.benhsoan.domain.vitalsign.enums.VitalSignAbnormalFlag;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;
import com.benhsoan.port.inbound.vitalsign.GetPatientVitalSignHistoryUseCase;
import com.benhsoan.port.inbound.vitalsign.GetVitalSignUseCase;
import com.benhsoan.port.inbound.vitalsign.RecordVitalSignUseCase;
import com.benhsoan.port.inbound.vitalsign.UpdateVitalSignUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = VitalSignController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({VitalSignRestMapper.class, GlobalExceptionHandler.class, RequirePermissionAspect.class,
        PermissionEvaluator.class, VitalSignControllerTest.AspectTestConfig.class})
@DisplayName("VitalSignController - MockMvc Tests")
class VitalSignControllerTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecordVitalSignUseCase recordVitalSignUseCase;

    @MockitoBean
    private UpdateVitalSignUseCase updateVitalSignUseCase;

    @MockitoBean
    private GetVitalSignUseCase getVitalSignUseCase;

    @MockitoBean
    private GetPatientVitalSignHistoryUseCase getPatientVitalSignHistoryUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private ClockPort clockPort;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-01: POST /vital-signs - 201 Created với dữ liệu hợp lệ")
    void recordVitalSignReturns201() throws Exception {
        UUID visitId = UUID.randomUUID();
        UUID vitalSignId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(recordVitalSignUseCase.record(any()))
                .thenReturn(new VitalSignResult(
                        vitalSignId, visitId, patientId, null, 75, 120, 80,
                        new BigDecimal("37.0"), 16, new BigDecimal("60.0"), new BigDecimal("165.0"),
                        new BigDecimal("22.0"), 98, false, List.of(), "Bình thường",
                        UUID.randomUUID(), Instant.now(), null, null
                ));

        String body = """
                {
                    "visitId": "%s",
                    "pulse": 75,
                    "bloodPressureSystolic": 120,
                    "bloodPressureDiastolic": 80,
                    "temperature": 37.0,
                    "respiratoryRate": 16,
                    "weight": 60.0,
                    "height": 165.0,
                    "spo2": 98,
                    "note": "Bình thường"
                }
                """.formatted(visitId);

        mockMvc.perform(post("/vital-signs")
                        .with(withPermission("VITAL_SIGN_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(vitalSignId.toString()))
                .andExpect(jsonPath("$.pulse").value(75))
                .andExpect(jsonPath("$.bloodPressureSystolic").value(120))
                .andExpect(jsonPath("$.abnormal").value(false));
    }

    @Test
    @DisplayName("TC-02: POST /vital-signs - 400 Bad Request khi nhiệt độ là 90 độ C")
    void recordVitalSignRejectsTemperature90C() throws Exception {
        UUID visitId = UUID.randomUUID();
        String body = """
                {
                    "visitId": "%s",
                    "temperature": 90.0
                }
                """.formatted(visitId);

        mockMvc.perform(post("/vital-signs")
                        .with(withPermission("VITAL_SIGN_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-03: POST /vital-signs - Trả về cờ cảnh báo bất thường khi huyết áp cao")
    void recordVitalSignReturnsAbnormalFlags() throws Exception {
        UUID visitId = UUID.randomUUID();
        UUID vitalSignId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(recordVitalSignUseCase.record(any()))
                .thenReturn(new VitalSignResult(
                        vitalSignId, visitId, patientId, null, 75, 150, 95,
                        new BigDecimal("37.0"), 16, new BigDecimal("60.0"), new BigDecimal("165.0"),
                        new BigDecimal("22.0"), 98, true, List.of(VitalSignAbnormalFlag.HYPERTENSION), "Huyết áp cao",
                        UUID.randomUUID(), Instant.now(), null, null
                ));

        String body = """
                {
                    "visitId": "%s",
                    "bloodPressureSystolic": 150,
                    "bloodPressureDiastolic": 95
                }
                """.formatted(visitId);

        mockMvc.perform(post("/vital-signs")
                        .with(withPermission("VITAL_SIGN_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abnormal").value(true))
                .andExpect(jsonPath("$.abnormalFlags[0]").value("HYPERTENSION"));
    }

    @Test
    @DisplayName("TC-04: GET /vital-signs/patients/{patientId}/history - 200 OK trả về danh sách lịch sử")
    void getPatientHistoryReturns200() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID visit1 = UUID.randomUUID();
        UUID visit2 = UUID.randomUUID();

        when(getPatientVitalSignHistoryUseCase.getHistory(patientId))
                .thenReturn(List.of(
                        new VitalSignResult(UUID.randomUUID(), visit1, patientId, null, 75, 120, 80,
                                new BigDecimal("37.0"), 16, new BigDecimal("60.0"), new BigDecimal("165.0"),
                                new BigDecimal("22.0"), 98, false, List.of(), null,
                                UUID.randomUUID(), Instant.parse("2026-08-01T10:00:00Z"), null, null),
                        new VitalSignResult(UUID.randomUUID(), visit2, patientId, null, 80, 125, 82,
                                new BigDecimal("37.1"), 16, new BigDecimal("60.5"), new BigDecimal("165.0"),
                                new BigDecimal("22.2"), 98, false, List.of(), null,
                                UUID.randomUUID(), Instant.parse("2026-08-15T10:00:00Z"), null, null)
                ));

        mockMvc.perform(get("/vital-signs/patients/{patientId}/history", patientId)
                        .with(withPermission("VITAL_SIGN_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pulse").value(75))
                .andExpect(jsonPath("$[1].pulse").value(80));
    }

    @Test
    @DisplayName("GET /vital-signs/visits/{visitId} - 200 OK")
    void getByVisitIdReturns200() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(getVitalSignUseCase.getByVisitId(visitId))
                .thenReturn(List.of(new VitalSignResult(
                        UUID.randomUUID(), visitId, UUID.randomUUID(), null, 75, 120, 80,
                        new BigDecimal("37.0"), 16, new BigDecimal("60.0"), new BigDecimal("165.0"),
                        new BigDecimal("22.0"), 98, false, List.of(), null,
                        UUID.randomUUID(), Instant.now(), null, null
                )));

        mockMvc.perform(get("/vital-signs/visits/{visitId}", visitId)
                        .with(withPermission("VITAL_SIGN_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pulse").value(75));
    }

    @Test
    @DisplayName("PUT /vital-signs/{id} - 200 OK cập nhật chỉ số sinh tồn")
    void updateVitalSignReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateVitalSignUseCase.update(eq(id), any()))
                .thenReturn(new VitalSignResult(
                        id, UUID.randomUUID(), UUID.randomUUID(), null, 72, 118, 78,
                        new BigDecimal("36.8"), 16, new BigDecimal("60.0"), new BigDecimal("165.0"),
                        new BigDecimal("22.0"), 98, false, List.of(), "Sau nghỉ ngơi",
                        UUID.randomUUID(), Instant.now(), UUID.randomUUID(), Instant.now()
                ));

        String body = """
                {
                    "pulse": 72,
                    "bloodPressureSystolic": 118,
                    "bloodPressureDiastolic": 78,
                    "temperature": 36.8,
                    "respiratoryRate": 16,
                    "weight": 60.0,
                    "height": 165.0,
                    "spo2": 98,
                    "note": "Sau nghỉ ngơi"
                }
                """;

        mockMvc.perform(put("/vital-signs/{id}", id)
                        .with(withPermission("VITAL_SIGN_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pulse").value(72))
                .andExpect(jsonPath("$.bloodPressureSystolic").value(118));
    }

    @Test
    @DisplayName("GET /vital-signs/{id} - 200 OK lấy chỉ số sinh tồn theo ID")
    void getByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(getVitalSignUseCase.getById(id))
                .thenReturn(new VitalSignResult(
                        id, UUID.randomUUID(), UUID.randomUUID(), null, 75, 120, 80,
                        new BigDecimal("37.0"), 16, new BigDecimal("60.0"), new BigDecimal("165.0"),
                        new BigDecimal("22.0"), 98, false, List.of(), "Bình thường",
                        UUID.randomUUID(), Instant.now(), null, null
                ));

        mockMvc.perform(get("/vital-signs/{id}", id)
                        .with(withPermission("VITAL_SIGN_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.pulse").value(75));
    }

    @Test
    @DisplayName("GET /vital-signs?patientId=... - 200 OK tìm kiếm theo patientId")
    void searchByPatientIdReturns200() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(getPatientVitalSignHistoryUseCase.getHistory(patientId))
                .thenReturn(List.of(new VitalSignResult(
                        UUID.randomUUID(), UUID.randomUUID(), patientId, null, 75, 120, 80,
                        new BigDecimal("37.0"), 16, new BigDecimal("60.0"), new BigDecimal("165.0"),
                        new BigDecimal("22.0"), 98, false, List.of(), null,
                        UUID.randomUUID(), Instant.now(), null, null
                )));

        mockMvc.perform(get("/vital-signs")
                        .param("patientId", patientId.toString())
                        .with(withPermission("VITAL_SIGN_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pulse").value(75));
    }

    @Test
    @DisplayName("GET /vital-signs?visitId=... - 200 OK tìm kiếm theo visitId")
    void searchByVisitIdReturns200() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(getVitalSignUseCase.getByVisitId(visitId))
                .thenReturn(List.of(new VitalSignResult(
                        UUID.randomUUID(), visitId, UUID.randomUUID(), null, 80, 120, 80,
                        new BigDecimal("37.0"), 16, new BigDecimal("60.0"), new BigDecimal("165.0"),
                        new BigDecimal("22.0"), 98, false, List.of(), null,
                        UUID.randomUUID(), Instant.now(), null, null
                )));

        mockMvc.perform(get("/vital-signs")
                        .param("visitId", visitId.toString())
                        .with(withPermission("VITAL_SIGN_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pulse").value(80));
    }

    @Test
    @DisplayName("GET /vital-signs - 200 OK trả về danh sách rỗng khi không có param")
    void searchWithoutParamsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/vital-signs")
                        .with(withPermission("VITAL_SIGN_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Chặn khi không có quyền thao tác (403 Forbidden)")
    void rejectsWhenPermissionMissing() throws Exception {
        mockMvc.perform(get("/vital-signs/visits/{visitId}", UUID.randomUUID())
                        .with(withPermission("DIFFERENT_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor withPermission(String permission) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "doctor-user", null, List.of(new SimpleGrantedAuthority("PERMISSION_" + permission))));
            return request;
        };
    }
}

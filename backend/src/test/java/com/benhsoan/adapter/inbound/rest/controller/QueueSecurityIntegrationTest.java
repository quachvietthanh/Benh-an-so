package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.benhsoan.adapter.inbound.rest.mapper.QueueRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.QueueCheckInResult;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.CallNextQueueItemUseCase;
import com.benhsoan.port.inbound.queue.CheckInAppointmentUseCase;
import com.benhsoan.port.inbound.queue.CheckInWalkInUseCase;
import com.benhsoan.port.inbound.queue.CompleteQueueItemUseCase;
import com.benhsoan.port.inbound.queue.GetMyQueueUseCase;
import com.benhsoan.port.inbound.queue.GetQueueItemUseCase;
import com.benhsoan.port.inbound.queue.GetQueuesUseCase;
import com.benhsoan.port.inbound.queue.SkipQueueItemUseCase;
import com.benhsoan.port.inbound.queue.UpdateQueueItemStatusUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = QueueController.class)
@Import({AnonymizationModeState.class, QueueRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class, RequirePermissionAspect.class, PermissionEvaluator.class,
        QueueSecurityIntegrationTest.AspectTestConfig.class})
class QueueSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetQueuesUseCase getQueuesUseCase;
    @MockitoBean private GetMyQueueUseCase getMyQueueUseCase;
    @MockitoBean private CheckInAppointmentUseCase checkInAppointmentUseCase;
    @MockitoBean private CheckInWalkInUseCase checkInWalkInUseCase;
    @MockitoBean private CallNextQueueItemUseCase callNextQueueItemUseCase;
    @MockitoBean private UpdateQueueItemStatusUseCase updateQueueItemStatusUseCase;
    @MockitoBean private CompleteQueueItemUseCase completeQueueItemUseCase;
    @MockitoBean private GetQueueItemUseCase getQueueItemUseCase;
    @MockitoBean private SkipQueueItemUseCase skipQueueItemUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    private static RequestPostProcessor permission(String role, String... codes) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        for (String code : codes) {
            authorities.add(new SimpleGrantedAuthority("PERMISSION_" + code));
        }
        return user("tester").roles(role).authorities(authorities);
    }

    @Test
    void returnsTheCommonErrorContractForSecurityFilterFailures() throws Exception {
        mockMvc.perform(get("/queues"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Bạn cần đăng nhập để truy cập tài nguyên này"))
                .andExpect(jsonPath("$.path").value("/queues"))
                .andExpect(jsonPath("$.details").isMap());

        mockMvc.perform(get("/queues")
                        .param("date", "2026-08-14")
                        .with(permission("DOCTOR", "PATIENT_READ")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/queues"));
    }

    @Test
    void allowsManagerToReadQueueWithQueueViewPermission() throws Exception {
        QueueItemResult queueItem = new QueueItemResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Nguyen Van A",
                UUID.randomUUID(),
                "Bac si B",
                UUID.randomUUID(),
                "P101",
                null,
                UUID.randomUUID(),
                "VIS000001",
                QueueItemSourceType.WALK_IN,
                QueueItemStatus.WAITING,
                1,
                LocalDate.of(2026, 8, 14),
                Instant.parse("2026-08-14T01:00:00Z"),
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(getQueuesUseCase.getQueues(any())).thenReturn(List.of(queueItem));
        when(getQueueItemUseCase.getById(any())).thenReturn(queueItem);

        mockMvc.perform(get("/queues")
                        .param("date", "2026-08-14")
                        .with(permission("MANAGER", "QUEUE_VIEW")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/queue-items/{itemId}", queueItem.id())
                        .with(permission("MANAGER", "QUEUE_VIEW")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/appointments/{appointmentId}/check-in", UUID.randomUUID())
                        .with(permission("MANAGER", "QUEUE_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/queues/{queueId}/call-next", UUID.randomUUID())
                        .with(permission("MANAGER", "QUEUE_VIEW")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsQueueOperationsWithProperPermissions() throws Exception {
        QueueItemResult queueItem = new QueueItemResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Nguyen Van A",
                UUID.randomUUID(),
                "Bac si B",
                UUID.randomUUID(),
                "P101",
                null,
                UUID.randomUUID(),
                "VIS000001",
                QueueItemSourceType.WALK_IN,
                QueueItemStatus.IN_PROGRESS,
                1,
                LocalDate.of(2026, 8, 14),
                Instant.parse("2026-08-14T01:00:00Z"),
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(callNextQueueItemUseCase.callNext(any())).thenReturn(queueItem);

        mockMvc.perform(post("/queues/{queueId}/call-next", UUID.randomUUID())
                        .with(permission("DOCTOR", "QUEUE_CALL_NEXT")))
                .andExpect(status().isOk());

        UUID appointmentId = UUID.randomUUID();
        QueueCheckInResult checkInResult = new QueueCheckInResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "VIS000120", appointmentId,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, LocalDate.of(2026, 8, 14),
                QueueItemSourceType.APPOINTMENT, QueueItemStatus.WAITING, VisitStatus.WAITING,
                Instant.parse("2026-08-14T01:00:00Z")
        );
        when(checkInAppointmentUseCase.checkIn(any())).thenReturn(checkInResult);

        mockMvc.perform(post("/appointments/{appointmentId}/check-in", appointmentId)
                        .with(permission("RECEPTIONIST", "QUEUE_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }
}

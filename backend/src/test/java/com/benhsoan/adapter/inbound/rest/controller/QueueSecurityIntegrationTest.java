package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.QueueRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
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
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = QueueController.class)
@Import({QueueRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class QueueSecurityIntegrationTest {

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
    @MockitoBean private ClockPort clockPort;

    @Test
    void allowsManagerToReadQueueButNotOperate() throws Exception {
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
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/queue-items/{itemId}", queueItem.id())
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/appointments/{appointmentId}/check-in", UUID.randomUUID())
                        .with(user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/queues/{queueId}/call-next", UUID.randomUUID())
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }
}

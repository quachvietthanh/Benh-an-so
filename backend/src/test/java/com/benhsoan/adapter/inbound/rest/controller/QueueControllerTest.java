package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.QueueRestMapper;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.port.dto.result.QueueCheckInResult;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.dto.command.queue.CheckInWalkInCommand;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.benhsoan.port.inbound.queue.CallNextQueueItemUseCase;
import com.benhsoan.port.inbound.queue.CheckInAppointmentUseCase;
import com.benhsoan.port.inbound.queue.CheckInWalkInUseCase;
import com.benhsoan.port.inbound.queue.CompleteQueueItemUseCase;
import com.benhsoan.port.inbound.queue.GetMyQueueUseCase;
import com.benhsoan.port.inbound.queue.GetQueueItemUseCase;
import com.benhsoan.port.inbound.queue.GetQueuesUseCase;
import com.benhsoan.port.inbound.queue.SkipQueueItemUseCase;
import com.benhsoan.port.inbound.queue.UpdateQueueItemStatusUseCase;

@WebMvcTest(controllers = QueueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({QueueRestMapper.class, GlobalExceptionHandler.class})
class QueueControllerTest {

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
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void checksInAppointment() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        UUID queueItemId = UUID.randomUUID();
        when(checkInAppointmentUseCase.checkIn(any())).thenReturn(new QueueCheckInResult(
                queueItemId, UUID.randomUUID(), UUID.randomUUID(), "VIS000120", appointmentId,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3, LocalDate.of(2026, 8, 9),
                QueueItemSourceType.APPOINTMENT, QueueItemStatus.WAITING, VisitStatus.WAITING,
                Instant.parse("2026-08-09T02:00:00Z")
        ));

        mockMvc.perform(post("/appointments/{appointmentId}/check-in", appointmentId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueItemId").value(queueItemId.toString()))
                .andExpect(jsonPath("$.appointmentId").value(appointmentId.toString()))
                .andExpect(jsonPath("$.visitCode").value("VIS000120"))
                .andExpect(jsonPath("$.sourceType").value("APPOINTMENT"));
    }

    @Test
    void acceptsLegacyWalkInPayloadWithoutSpecialty() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        when(checkInWalkInUseCase.checkIn(any())).thenReturn(new QueueCheckInResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "VIS000121", null,
                patientId, doctorId, UUID.randomUUID(), 4, LocalDate.of(2026, 8, 9),
                QueueItemSourceType.WALK_IN, QueueItemStatus.WAITING, VisitStatus.WAITING,
                Instant.parse("2026-08-09T02:00:00Z")
        ));

        mockMvc.perform(post("/queue-items/walk-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"doctorId\":\"" + doctorId
                                + "\",\"reason\":\"Kham tong quat\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitCode").value("VIS000121"));

        ArgumentCaptor<CheckInWalkInCommand> captor = ArgumentCaptor.forClass(CheckInWalkInCommand.class);
        org.mockito.Mockito.verify(checkInWalkInUseCase).checkIn(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(null, captor.getValue().specialtyId());
    }

    @Test
    void forwardsSpecialtyFromWalkInPayload() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        when(checkInWalkInUseCase.checkIn(any())).thenReturn(new QueueCheckInResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "VIS000122", null,
                patientId, doctorId, UUID.randomUUID(), 4, LocalDate.of(2026, 8, 9),
                QueueItemSourceType.WALK_IN, QueueItemStatus.WAITING, VisitStatus.WAITING,
                Instant.parse("2026-08-09T02:00:00Z")
        ));

        mockMvc.perform(post("/queue-items/walk-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"doctorId\":\"" + doctorId
                                + "\",\"reason\":\"Kham noi\",\"specialtyId\":\"" + specialtyId + "\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<CheckInWalkInCommand> captor = ArgumentCaptor.forClass(CheckInWalkInCommand.class);
        org.mockito.Mockito.verify(checkInWalkInUseCase).checkIn(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(specialtyId, captor.getValue().specialtyId());
    }

    @Test
    void returnsTheStandardNotFoundErrorForUnknownOrInactiveSpecialty() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        when(checkInWalkInUseCase.checkIn(any())).thenThrow(new SpecialtyNotFoundException(specialtyId));

        for (UUID invalidSpecialtyId : java.util.List.of(specialtyId, UUID.randomUUID())) {
            mockMvc.perform(post("/queue-items/walk-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"patientId\":\"" + patientId + "\",\"doctorId\":\"" + doctorId
                                    + "\",\"reason\":\"Kham noi\",\"specialtyId\":\"" + invalidSpecialtyId + "\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SPECIALTY_NOT_FOUND"));
        }
    }

    @Test
    void skipsQueueItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(skipQueueItemUseCase.skip(any())).thenReturn(result(itemId));

        mockMvc.perform(post("/queue-items/{itemId}/skip", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Patient absent when called"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.patientName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.doctorName").value("Bac si Nguyen Van B"))
                .andExpect(jsonPath("$.roomNumber").value("P101"))
                .andExpect(jsonPath("$.visitCode").value("VIS000100"))
                .andExpect(jsonPath("$.skipReason").value("Patient absent when called"));
    }

    @Test
    void rejectsBlankSkipReason() throws Exception {
        mockMvc.perform(post("/queue-items/{itemId}/skip", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":" "}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(skipQueueItemUseCase);
    }

    private QueueItemResult result(UUID itemId) {
        Instant now = Instant.parse("2026-08-02T02:00:00Z");
        return new QueueItemResult(
                itemId, UUID.randomUUID(), UUID.randomUUID(), "Nguyen Van A",
                UUID.randomUUID(), "Bac si Nguyen Van B", UUID.randomUUID(), "P101",
                null, UUID.randomUUID(), "VIS000100", QueueItemSourceType.WALK_IN,
                QueueItemStatus.SKIPPED, 1, LocalDate.of(2026, 8, 2), now,
                now, null, null, null, now, "Patient absent when called"
        );
    }
}

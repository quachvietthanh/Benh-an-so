package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import com.benhsoan.port.dto.result.PageResponse;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalQueueRestMapper;
import com.benhsoan.domain.queue.enums.PriorityLevel;
import com.benhsoan.domain.queue.enums.QueueStatus;
import com.benhsoan.port.dto.result.QueueResult;
import com.benhsoan.port.inbound.queue.AddToQueueUseCase;
import com.benhsoan.port.inbound.queue.CallNextUseCase;
import com.benhsoan.port.inbound.queue.GetQueueListUseCase;
import com.benhsoan.port.inbound.queue.UpdateQueueStatusUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalQueueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MedicalQueueRestMapper.class)
@DisplayName("MedicalQueueController - MockMvc Tests")
class MedicalQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddToQueueUseCase addToQueueUseCase;

    @MockitoBean
    private CallNextUseCase callNextUseCase;

    @MockitoBean
    private UpdateQueueStatusUseCase updateQueueStatusUseCase;

    @MockitoBean
    private GetQueueListUseCase getQueueListUseCase;

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

    private final UUID queueId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID createdBy = UUID.randomUUID();

    private QueueResult sampleResult() {
        return new QueueResult(
                queueId, patientId, doctorId, "Room 101", 1,
                QueueStatus.WAITING, PriorityLevel.REGULAR, null,
                Instant.now(), null, null, null, null, null, null,
                createdBy, Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("POST /queue")
    class AddToQueue {

        @Test
        @DisplayName("Should return 201 Created")
        void addToQueueReturns201() throws Exception {
            when(currentUserPort.getCurrentUserId()).thenReturn(createdBy);
            when(addToQueueUseCase.addToQueue(any())).thenReturn(sampleResult());

            String body = """
                    {
                        "patientId": "%s",
                        "priorityLevel": "REGULAR",
                        "roomNumber": "Room 101"
                    }
                    """.formatted(patientId.toString());

            mockMvc.perform(post("/queue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(queueId.toString()))
                    .andExpect(jsonPath("$.queueNumber").value(1))
                    .andExpect(jsonPath("$.status").value("WAITING"));
        }

        @Test
        @DisplayName("Should return 400 when patientId is null")
        void addToQueueMissingPatientId() throws Exception {
            String body = """
                    {
                        "priorityLevel": "REGULAR",
                        "roomNumber": "Room 101"
                    }
                    """;

            mockMvc.perform(post("/queue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when roomNumber is null")
        void addToQueueMissingRoomNumber() throws Exception {
            String body = """
                    {
                        "patientId": "%s",
                        "priorityLevel": "REGULAR"
                    }
                    """.formatted(patientId.toString());

            mockMvc.perform(post("/queue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /queue/call-next")
    class CallNext {

        @Test
        @DisplayName("Should return 200 OK")
        void callNextReturns200() throws Exception {
            QueueResult inProgress = new QueueResult(
                    queueId, patientId, doctorId, "Room 101", 1,
                    QueueStatus.IN_PROGRESS, PriorityLevel.REGULAR, null,
                    Instant.now(), Instant.now(), Instant.now(),
                    null, null, null, null,
                    createdBy, Instant.now(), Instant.now()
            );

            when(callNextUseCase.callNext(any())).thenReturn(inProgress);

            String body = """
                    {
                        "doctorId": "%s",
                        "roomNumber": "Room 101"
                    }
                    """.formatted(doctorId.toString());

            mockMvc.perform(post("/queue/call-next")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.doctorId").value(doctorId.toString()));
        }

        @Test
        @DisplayName("Should return 400 when doctorId is null")
        void callNextMissingDoctorId() throws Exception {
            String body = """
                    {
                        "roomNumber": "Room 101"
                    }
                    """;

            mockMvc.perform(post("/queue/call-next")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when roomNumber is null")
        void callNextMissingRoomNumber() throws Exception {
            String body = """
                    {
                        "doctorId": "%s"
                    }
                    """.formatted(doctorId.toString());

            mockMvc.perform(post("/queue/call-next")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /queue/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("Should return 200 OK")
        void updateStatusReturns200() throws Exception {
            QueueResult completed = new QueueResult(
                    queueId, patientId, doctorId, "Room 101", 1,
                    QueueStatus.COMPLETED, PriorityLevel.REGULAR, null,
                    Instant.now(), Instant.now(), Instant.now(),
                    null, Instant.now(), null, null,
                    createdBy, Instant.now(), Instant.now()
            );

            when(updateQueueStatusUseCase.updateStatus(any())).thenReturn(completed);

            String body = """
                    {
                        "newStatus": "COMPLETED"
                    }
                    """;

            mockMvc.perform(put("/queue/{id}/status", queueId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should return 400 when newStatus is null")
        void updateStatusMissingStatus() throws Exception {
            String body = "{}";

            mockMvc.perform(put("/queue/{id}/status", queueId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /queue/room/{roomNumber}")
    class GetQueueByRoom {

        @Test
        @DisplayName("Should return 200 OK with queue list")
        void getQueueByRoomReturns200() throws Exception {
            when(getQueueListUseCase.getQueueList(any()))
                    .thenReturn(PageResponse.of(List.of(sampleResult()), 0, 20, 1));

            mockMvc.perform(get("/queue/room/{roomNumber}", "Room 101"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].queueNumber").value(1))
                    .andExpect(jsonPath("$.content[0].roomNumber").value("Room 101"));
        }
    }

    @Nested
    @DisplayName("GET /queue/doctor/{doctorId}")
    class GetQueueByDoctor {

        @Test
        @DisplayName("Should return 200 OK with queue list")
        void getQueueByDoctorReturns200() throws Exception {
            when(getQueueListUseCase.getQueueList(any()))
                    .thenReturn(PageResponse.of(List.of(sampleResult()), 0, 20, 1));

            mockMvc.perform(get("/queue/doctor/{doctorId}", doctorId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].queueNumber").value(1));
        }
    }

    @Nested
    @DisplayName("GET /queue/count")
    class Count {

        @Test
        @DisplayName("Should return count")
        void countReturns200() throws Exception {
            when(getQueueListUseCase.count(any())).thenReturn(3L);

            mockMvc.perform(get("/queue/count")
                            .param("roomNumber", "Room 101")
                            .param("status", "WAITING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(3));
        }
    }
}

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
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.FollowUpReminderRestMapper;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.inbound.followup.CreateFollowUpReminderUseCase;
import com.benhsoan.port.inbound.followup.GetDueFollowUpRemindersUseCase;
import com.benhsoan.port.inbound.followup.SearchFollowUpRemindersUseCase;
import com.benhsoan.port.inbound.followup.UpdateFollowUpReminderStatusUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = FollowUpReminderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(FollowUpReminderRestMapper.class)
class FollowUpReminderControllerTest {

    private static final UUID REMINDER_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateFollowUpReminderUseCase createFollowUpReminderUseCase;
    @MockitoBean
    private GetDueFollowUpRemindersUseCase getDueFollowUpRemindersUseCase;
    @MockitoBean
    private SearchFollowUpRemindersUseCase searchFollowUpRemindersUseCase;
    @MockitoBean
    private UpdateFollowUpReminderStatusUseCase updateFollowUpReminderStatusUseCase;

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

    @Test
    void createsReminder() throws Exception {
        when(createFollowUpReminderUseCase.create(any())).thenReturn(result(ReminderStatus.PENDING));

        mockMvc.perform(post("/follow-up-reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "%s",
                                  "followUpDate": "2026-08-30",
                                  "remindAt": "2026-08-15T08:00:00Z",
                                  "reminderType": "REVISIT"
                                }
                                """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reminderType").value("REVISIT"));
    }

    @Test
    void rejectsCreateWithoutFollowUpDate() throws Exception {
        mockMvc.perform(post("/follow-up-reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "%s",
                                  "remindAt": "2026-08-15T08:00:00Z"
                                }
                                """.formatted(PATIENT_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsDueReminders() throws Exception {
        when(getDueFollowUpRemindersUseCase.getDue(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(result(ReminderStatus.PENDING)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/follow-up-reminders/due"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }
    @Test
    void searchesReminders() throws Exception {
        when(searchFollowUpRemindersUseCase.search(any()))
                .thenReturn(new PageImpl<>(List.of(result(ReminderStatus.SENT)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/follow-up-reminders").param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SENT"));
    }

    @Test
    void updatesStatus() throws Exception {
        when(updateFollowUpReminderStatusUseCase.updateStatus(any(), any()))
                .thenReturn(result(ReminderStatus.COMPLETED));

        mockMvc.perform(patch("/follow-up-reminders/{id}/status", REMINDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void rejectsFromAfterTo() throws Exception {
        mockMvc.perform(get("/follow-up-reminders/due")
                        .param("from", "2026-08-20")
                        .param("to", "2026-08-10"))
                .andExpect(status().isBadRequest());
    }

    private FollowUpReminderResult result(ReminderStatus status) {
        return new FollowUpReminderResult(
                REMINDER_ID,
                PATIENT_ID,
                null,
                null,
                LocalDate.of(2026, 8, 30),
                Instant.parse("2026-08-15T08:00:00Z"),
                ReminderType.REVISIT,
                status,
                "Recheck",
                UUID.randomUUID(),
                Instant.parse("2026-08-15T08:00:00Z")
        );
    }
}

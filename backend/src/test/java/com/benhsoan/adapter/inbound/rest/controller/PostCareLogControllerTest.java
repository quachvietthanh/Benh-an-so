package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PostCareLogRestMapper;
import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;
import com.benhsoan.port.dto.result.PostCareLogResult;
import com.benhsoan.port.inbound.carelog.CreatePostCareLogUseCase;
import com.benhsoan.port.inbound.carelog.GetPatientCareLogsUseCase;
import com.benhsoan.port.inbound.carelog.SearchPostCareLogsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PostCareLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PostCareLogRestMapper.class)
class PostCareLogControllerTest {

    private static final UUID CARE_LOG_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePostCareLogUseCase createPostCareLogUseCase;
    @MockitoBean
    private GetPatientCareLogsUseCase getPatientCareLogsUseCase;
    @MockitoBean
    private SearchPostCareLogsUseCase searchPostCareLogsUseCase;

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
    void createsCareLog() throws Exception {
        when(createPostCareLogUseCase.create(any())).thenReturn(result());

        mockMvc.perform(post("/care-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactChannel").value("PHONE"))
                .andExpect(jsonPath("$.contactOutcome").value("REACHED"));
    }

    @Test
    void rejectsCreateWithoutCareNotes() throws Exception {
        mockMvc.perform(post("/care-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "%s",
                                  "contactChannel": "PHONE",
                                  "contactedAt": "2026-08-15T08:00:00Z",
                                  "patientCondition": "STABLE",
                                  "careNotes": "",
                                  "contactOutcome": "REACHED"
                                }
                                """.formatted(PATIENT_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreateWithoutPatientId() throws Exception {
        mockMvc.perform(post("/care-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contactChannel": "PHONE",
                                  "contactedAt": "2026-08-15T08:00:00Z",
                                  "patientCondition": "STABLE",
                                  "careNotes": "notes",
                                  "contactOutcome": "REACHED"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsPatientCareLogs() throws Exception {
        when(getPatientCareLogsUseCase.getForPatient(PATIENT_ID)).thenReturn(List.of(result()));

        mockMvc.perform(get("/care-logs/patient/{patientId}", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactChannel").value("PHONE"));
    }

    @Test
    void searchesCareLogs() throws Exception {
        when(searchPostCareLogsUseCase.search(any()))
                .thenReturn(new PageImpl<>(List.of(result()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/care-logs").param("channel", "PHONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contactChannel").value("PHONE"));
    }

    @Test
    void rejectsFromAfterTo() throws Exception {
        mockMvc.perform(get("/care-logs")
                        .param("from", "2026-08-20T00:00:00Z")
                        .param("to", "2026-08-10T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    private String createBody() {
        return """
                {
                  "patientId": "%s",
                  "contactChannel": "PHONE",
                  "contactedAt": "2026-08-15T08:00:00Z",
                  "patientCondition": "STABLE",
                  "careNotes": "Benh nhan on dinh",
                  "contactOutcome": "REACHED"
                }
                """.formatted(PATIENT_ID);
    }

    private PostCareLogResult result() {
        return new PostCareLogResult(
                CARE_LOG_ID,
                PATIENT_ID,
                null,
                null,
                ContactChannel.PHONE,
                Instant.parse("2026-08-15T08:00:00Z"),
                PatientCondition.STABLE,
                "Benh nhan on dinh",
                ContactOutcome.REACHED,
                UUID.randomUUID(),
                Instant.parse("2026-08-15T08:00:00Z")
        );
    }
}

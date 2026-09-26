package com.benhsoan.application.ucservice.carelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.carelog.PostCareLog;
import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.result.PostCareLogResult;
import com.benhsoan.port.outbound.repository.carelog.PostCareLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class GetPatientCareLogsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private final PostCareLogRepository postCareLogRepository = mock(PostCareLogRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final PostCareLogAuthorizer authorizer = new PostCareLogAuthorizer(currentUserPort);
    private final PostCareLogResultMapper resultMapper = new PostCareLogResultMapper();

    private GetPatientCareLogsService service;

    @BeforeEach
    void setUp() {
        service = new GetPatientCareLogsService(
                postCareLogRepository, patientRepository, resultMapper, authorizer);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(mock(Patient.class)));
    }

    @Test
    void returnsCareHistoryOrderedByContactedAtDesc() {
        PostCareLog older = PostCareLog.create(
                PATIENT_ID, null, null, ContactChannel.PHONE, NOW.minusSeconds(60),
                PatientCondition.STABLE, "older", ContactOutcome.REACHED, ACTOR, NOW.minusSeconds(60));
        PostCareLog newer = PostCareLog.create(
                PATIENT_ID, null, null, ContactChannel.ZALO, NOW,
                PatientCondition.RECOVERING, "newer", ContactOutcome.REACHED, ACTOR, NOW);
        when(postCareLogRepository.findByPatientIdOrderByContactedAtDesc(PATIENT_ID))
                .thenReturn(List.of(newer, older));

        List<PostCareLogResult> results = service.getForPatient(PATIENT_ID);

        assertEquals(2, results.size());
        assertEquals(newer.getId(), results.get(0).id());
        assertEquals(older.getId(), results.get(1).id());
    }

    @Test
    void rejectsUnknownPatient() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.getForPatient(PATIENT_ID));
    }

    @Test
    void rejectsUnauthorizedRole() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.getForPatient(PATIENT_ID));
    }
}

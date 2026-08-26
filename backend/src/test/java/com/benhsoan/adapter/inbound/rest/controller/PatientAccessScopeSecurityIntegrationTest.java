package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.application.ucservice.patient.PatientAccessDeniedAuditWriter;
import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TC-03 / QTN-23: a logged-in PATIENT attempting to access another patient's resource is
 * rejected with HTTP 403 and an ACCESS_DENIED audit entry is written (via the real guard +
 * denial writer wired to a test controller).
 */
@ExtendWith(MockitoExtension.class)
class PatientAccessScopeSecurityIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private ClockPort clockPort;

    @Mock
    private AuditLogRepository auditLogRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PatientAccessDeniedAuditWriter denialAuditWriter =
                new PatientAccessDeniedAuditWriter(auditLogRepository, new ObjectMapper());
        PatientAccessGuard guard =
                new PatientAccessGuard(currentUserPort, patientRepository, denialAuditWriter, clockPort);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PatientScopeController(guard))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class PatientScopeController {

        private final PatientAccessGuard guard;

        PatientScopeController(PatientAccessGuard guard) {
            this.guard = guard;
        }

        @GetMapping("/patient-portal/records/{patientId}")
        public Map<String, String> getRecords(@PathVariable UUID patientId) {
            Patient patient = guard.requirePatientOwnership(patientId);
            return Map.of("patientId", patient.getId().toString());
        }
    }

    @Test
    void patientAccessingOtherPatientsDataReturns403AndWritesDenialAudit() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID ownPatientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();
        Patient own = mockPatient(ownPatientId);

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(clockPort.now()).thenReturn(NOW);

        mockMvc.perform(get("/patient-portal/records/{patientId}", otherPatientId))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(ActionType.ACCESS_DENIED, log.getActionType());
        assertEquals(ResourceType.PATIENT, log.getResourceType());
        assertEquals(otherPatientId, log.getResourceId());
        assertEquals(userId, log.getUserId());
    }

    @Test
    void patientAccessingOwnDataReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID ownPatientId = UUID.randomUUID();
        Patient own = mockPatient(ownPatientId);

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));

        mockMvc.perform(get("/patient-portal/records/{patientId}", ownPatientId))
                .andExpect(status().isOk());
    }

    private Patient mockPatient(UUID id) {
        Patient patient = Mockito.mock(Patient.class);
        when(patient.getId()).thenReturn(id);
        return patient;
    }
}


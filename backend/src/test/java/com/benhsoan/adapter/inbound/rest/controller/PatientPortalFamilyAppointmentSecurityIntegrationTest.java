package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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
 * NCL-14-CN-010 CV-04: request-level proof that a client-supplied {@code patientId} is never
 * sufficient authorisation. The real {@link PatientAccessGuard} plus the real
 * {@link PatientAccessDeniedAuditWriter} are wired behind a controller, so a refused
 * cross-patient read produces HTTP 403 AND an ACCESS_DENIED audit row for the same request.
 */
@ExtendWith(MockitoExtension.class)
class PatientPortalFamilyAppointmentSecurityIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-25T02:00:00Z");

    @Mock private PatientRepository patientRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;
    @Mock private AuditLogRepository auditLogRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PatientAccessDeniedAuditWriter denialAuditWriter =
                new PatientAccessDeniedAuditWriter(auditLogRepository, new ObjectMapper());
        PatientAccessGuard guard = new PatientAccessGuard(
                currentUserPort, patientRepository, denialAuditWriter, clockPort);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FamilyScopeController(guard))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class FamilyScopeController {

        private final PatientAccessGuard guard;

        FamilyScopeController(PatientAccessGuard guard) {
            this.guard = guard;
        }

        @GetMapping("/patient-portal/appointments")
        Map<String, String> list(@RequestParam UUID patientId) {
            Patient patient = guard.requirePatientAccess(patientId);
            return Map.of("patientId", patient.getId().toString());
        }

        @GetMapping("/patient-portal/appointments/{appointmentId}/owner/{ownerId}")
        Map<String, String> detail(
                @PathVariable UUID appointmentId,
                @PathVariable UUID ownerId,
                @RequestParam UUID patientId
        ) {
            Patient authorised = guard.requirePatientAccess(
                    patientId, ResourceType.APPOINTMENT, appointmentId);

            if (!ownerId.equals(authorised.getId())) {
                guard.denyPatientAccess(ownerId, ResourceType.APPOINTMENT, appointmentId);
                throw new AccessDeniedException(
                        "Appointment does not belong to the authorised patient.");
            }

            return Map.of("patientId", authorised.getId().toString());
        }
    }

    private Patient mockPatient(UUID id) {
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(id);
        return patient;
    }

    private AuditLog captureSingleAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void ownPatientScopeReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID ownId = UUID.randomUUID();
        Patient own = mockPatient(ownId);

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));

        mockMvc.perform(get("/patient-portal/appointments")
                        .param("patientId", ownId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void linkedDependentScopeReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();
        Patient own = mockPatient(UUID.randomUUID());
        Patient dependent = mockPatient(dependentId);

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(patientRepository.findByGuardianUserIdAndId(userId, dependentId))
                .thenReturn(Optional.of(dependent));

        mockMvc.perform(get("/patient-portal/appointments")
                        .param("patientId", dependentId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void unrelatedPatientIdReturns403AndWritesAccessDeniedAudit() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        Patient own = mockPatient(UUID.randomUUID());

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(patientRepository.findByGuardianUserIdAndId(userId, strangerId))
                .thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        mockMvc.perform(get("/patient-portal/appointments")
                        .param("patientId", strangerId.toString()))
                .andExpect(status().isForbidden());

        AuditLog log = captureSingleAuditLog();
        assertEquals(ActionType.ACCESS_DENIED, log.getActionType());
        assertEquals(ResourceType.PATIENT, log.getResourceType());
        assertEquals(strangerId, log.getResourceId());
        assertEquals(userId, log.getUserId());
    }

    @Test
    void noLinkedProfileAndUnrelatedScopeReturns403AndWritesAudit() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(patientRepository.findByGuardianUserIdAndId(userId, targetId))
                .thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        mockMvc.perform(get("/patient-portal/appointments")
                        .param("patientId", targetId.toString()))
                .andExpect(status().isForbidden());

        AuditLog log = captureSingleAuditLog();
        assertEquals(ActionType.ACCESS_DENIED, log.getActionType());
        assertEquals(targetId, log.getResourceId());
    }

    @Test
    void dependentOwnedAppointmentReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Patient own = mockPatient(UUID.randomUUID());
        Patient dependent = mockPatient(dependentId);

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(patientRepository.findByGuardianUserIdAndId(userId, dependentId))
                .thenReturn(Optional.of(dependent));

        mockMvc.perform(get("/patient-portal/appointments/{id}/owner/{owner}",
                        appointmentId, dependentId)
                        .param("patientId", dependentId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void idorScopeMismatchIsDeniedAndAuditedAsAppointmentResource() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();
        UUID thirdPartyPatientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Patient own = mockPatient(UUID.randomUUID());
        Patient dependent = mockPatient(dependentId);

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(patientRepository.findByGuardianUserIdAndId(userId, dependentId))
                .thenReturn(Optional.of(dependent));
        when(clockPort.now()).thenReturn(NOW);

        mockMvc.perform(get("/patient-portal/appointments/{id}/owner/{owner}",
                        appointmentId, thirdPartyPatientId)
                        .param("patientId", dependentId.toString()))
                .andExpect(status().isForbidden());

        AuditLog log = captureSingleAuditLog();
        assertEquals(ActionType.ACCESS_DENIED, log.getActionType());
        assertEquals(ResourceType.APPOINTMENT, log.getResourceType());
        assertEquals(appointmentId, log.getResourceId());
        assertEquals(userId, log.getUserId());
        assertEquals(thirdPartyPatientId.toString(),
                new ObjectMapper().readTree(log.getDetail()).get("targetPatientId").asText());
    }
}

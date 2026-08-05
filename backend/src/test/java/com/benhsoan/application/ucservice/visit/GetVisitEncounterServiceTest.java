package com.benhsoan.application.ucservice.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitEncounterAccessDeniedException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.VisitEncounterResult;
import com.benhsoan.port.outbound.repository.visit.VisitEncounterQueryRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class GetVisitEncounterServiceTest {

    @Test
    void returnsEncounterForOwningDoctor() {
        UUID doctorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        VisitEncounterQueryRepository repository = mock(VisitEncounterQueryRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        VisitEncounterResult encounter = encounter(visitId, doctorId);
        when(repository.findByVisitId(visitId)).thenReturn(Optional.of(encounter));
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);

        var result = service(repository, currentUserPort).getEncounter(visitId);

        assertEquals(visitId, result.visit().id());
        assertEquals("VIS000100", result.visit().visitCode());
    }

    @Test
    void rejectsDoctorViewingAnotherDoctorsEncounter() {
        UUID visitId = UUID.randomUUID();
        VisitEncounterQueryRepository repository = mock(VisitEncounterQueryRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(repository.findByVisitId(visitId)).thenReturn(Optional.of(encounter(visitId, UUID.randomUUID())));
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        assertThrows(VisitEncounterAccessDeniedException.class, () -> service(repository, currentUserPort)
                .getEncounter(visitId));
    }

    @Test
    void allowsAdminAndNurseToViewEncounter() {
        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        VisitEncounterQueryRepository repository = mock(VisitEncounterQueryRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(repository.findByVisitId(visitId)).thenReturn(Optional.of(encounter(visitId, doctorId)));
        when(currentUserPort.hasRole("NURSE")).thenReturn(true);

        assertEquals(visitId, service(repository, currentUserPort).getEncounter(visitId).visit().id());
    }

    @Test
    void rejectsReceptionistFromViewingEncounter() {
        UUID visitId = UUID.randomUUID();
        VisitEncounterQueryRepository repository = mock(VisitEncounterQueryRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(repository.findByVisitId(visitId)).thenReturn(Optional.of(encounter(visitId, UUID.randomUUID())));
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);

        assertThrows(VisitEncounterAccessDeniedException.class, () -> service(repository, currentUserPort)
                .getEncounter(visitId));
    }

    @Test
    void returnsNotFoundForUnknownVisit() {
        UUID visitId = UUID.randomUUID();
        VisitEncounterQueryRepository repository = mock(VisitEncounterQueryRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(repository.findByVisitId(visitId)).thenReturn(Optional.empty());

        assertThrows(VisitNotFoundException.class, () -> service(repository, currentUserPort).getEncounter(visitId));
    }

    private GetVisitEncounterService service(VisitEncounterQueryRepository repository, CurrentUserPort currentUserPort) {
        return new GetVisitEncounterService(repository, new VisitEncounterAuthorization(currentUserPort));
    }

    private VisitEncounterResult encounter(UUID visitId, UUID doctorId) {
        Instant now = Instant.parse("2026-08-03T02:00:00Z");
        return new VisitEncounterResult(
                new VisitEncounterResult.VisitInfo(
                        visitId, "VIS000100", VisitType.WALK_IN, VisitStatus.IN_PROGRESS,
                        now, now.plusSeconds(60), "Kham tong quat", "Theo doi"),
                new VisitEncounterResult.PatientInfo(
                        UUID.randomUUID(), "BN000100", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                        Gender.MALE, "0900000000"),
                new VisitEncounterResult.DoctorInfo(doctorId, "Bac si Nguyen Van B"),
                null,
                null,
                null,
                null);
    }
}

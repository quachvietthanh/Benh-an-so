package com.benhsoan.application.ucservice.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.VisitHandover;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitEncounterAccessDeniedException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.VisitHandoverResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.visit.VisitHandoverRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class GetVisitHandoversServiceTest {

    private CurrentUserPort currentUserPort;
    private VisitRepository visitRepository;
    private VisitHandoverRepository visitHandoverRepository;
    private UserRepository userRepository;

    private GetVisitHandoversService service;

    private final UUID doctorAId = UUID.randomUUID();
    private final UUID doctorBId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID visitId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-17T10:00:00Z");

    @BeforeEach
    void setUp() {
        currentUserPort = mock(CurrentUserPort.class);
        visitRepository = mock(VisitRepository.class);
        visitHandoverRepository = mock(VisitHandoverRepository.class);
        userRepository = mock(UserRepository.class);

        service = new GetVisitHandoversService(
                currentUserPort,
                visitRepository,
                visitHandoverRepository,
                userRepository
        );
    }

    private Visit createActiveVisit() {
        Visit v = Visit.create("VIS001", patientId, doctorAId, null, null, VisitType.WALK_IN, now, "Kham dau dau", null, doctorAId);
        v.start(now);
        return v;
    }

    @Test
    void returnsHandoversForCurrentDoctor() {
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorBId);

        Visit visit = createActiveVisit();
        visit.handover(doctorBId, "Chuyen bac si B", now);
        UUID currentVisitId = visit.getId();
        when(visitRepository.findById(currentVisitId)).thenReturn(Optional.of(visit));

        VisitHandover handover = VisitHandover.create(currentVisitId, doctorAId, doctorBId, "Chuyen khoa", doctorAId, now);
        when(visitHandoverRepository.findByVisitId(currentVisitId)).thenReturn(List.of(handover));

        User doctorA = User.restore(doctorAId, "docA", "pass", "Dr. Alice", "a@example.com", "0900000001",
                RoleConstants.DOCTOR, true, null, now);
        User doctorB = User.restore(doctorBId, "docB", "pass", "Dr. Bob", "b@example.com", "0900000002",
                RoleConstants.DOCTOR, true, null, now);
        when(userRepository.findAllById(anyList())).thenReturn(List.of(doctorA, doctorB));

        List<VisitHandoverResult> results = service.getHandovers(currentVisitId);

        assertEquals(1, results.size());
        assertEquals("Dr. Alice", results.get(0).fromDoctorName());
        assertEquals("Dr. Bob", results.get(0).toDoctorName());
        assertEquals("Chuyen khoa", results.get(0).reason());
    }

    @Test
    void returnsHandoversForInitialDoctor() {
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);

        Visit visit = createActiveVisit();
        visit.handover(doctorBId, "Chuyen bac si B", now);
        UUID currentVisitId = visit.getId();
        when(visitRepository.findById(currentVisitId)).thenReturn(Optional.of(visit));
        when(visitHandoverRepository.findByVisitId(currentVisitId)).thenReturn(List.of());

        List<VisitHandoverResult> results = service.getHandovers(currentVisitId);
        assertTrue(results.isEmpty());
    }

    @Test
    void rejectsUnrelatedDoctorWithoutPermission() {
        UUID strangerId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(strangerId);

        Visit visit = createActiveVisit();
        visit.handover(doctorBId, "Chuyen bac si B", now);
        UUID currentVisitId = visit.getId();
        when(visitRepository.findById(currentVisitId)).thenReturn(Optional.of(visit));
        when(visitHandoverRepository.findByVisitId(currentVisitId)).thenReturn(List.of());

        assertThrows(VisitEncounterAccessDeniedException.class, () -> service.getHandovers(currentVisitId));
    }

    @Test
    void rejectsUnrelatedDoctorEvenWithHandoverPermission() {
        UUID strangerId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(strangerId);
        when(currentUserPort.hasPermission("MEDICAL_RECORD_HANDOVER")).thenReturn(true);

        Visit visit = createActiveVisit();
        visit.handover(doctorBId, "Chuyen bac si B", now);
        UUID currentVisitId = visit.getId();
        when(visitRepository.findById(currentVisitId)).thenReturn(Optional.of(visit));
        when(visitHandoverRepository.findByVisitId(currentVisitId)).thenReturn(List.of());

        assertThrows(VisitEncounterAccessDeniedException.class, () -> service.getHandovers(currentVisitId));
    }

    @Test
    void rejectsWhenVisitNotFound() {
        when(visitRepository.findById(visitId)).thenReturn(Optional.empty());
        assertThrows(VisitNotFoundException.class, () -> service.getHandovers(visitId));
    }
}

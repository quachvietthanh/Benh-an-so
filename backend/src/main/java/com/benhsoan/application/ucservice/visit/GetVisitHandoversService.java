package com.benhsoan.application.ucservice.visit;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.VisitHandover;
import com.benhsoan.domain.visit.exception.VisitEncounterAccessDeniedException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.VisitHandoverResult;
import com.benhsoan.port.inbound.visit.GetVisitHandoversUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.visit.VisitHandoverRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVisitHandoversService implements GetVisitHandoversUseCase {

    private final CurrentUserPort currentUserPort;
    private final VisitRepository visitRepository;
    private final VisitHandoverRepository visitHandoverRepository;
    private final UserRepository userRepository;

    @Override
    public List<VisitHandoverResult> getHandovers(UUID visitId) {
        if (visitId == null) {
            throw new ValidationException("Visit ID is required");
        }

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));

        UUID actorId = currentUserPort.getCurrentUserId();
        boolean isAdmin = currentUserPort.hasRole("ADMIN");
        boolean isCurrentDoctor = visit.getDoctorId().equals(actorId);
        boolean isInitialDoctor = visit.getInitialDoctorId() != null && visit.getInitialDoctorId().equals(actorId);

        List<VisitHandover> handovers = visitHandoverRepository.findByVisitId(visitId);

        boolean isHandoverParticipant = handovers.stream()
                .anyMatch(h -> h.getFromDoctorId().equals(actorId) || h.getToDoctorId().equals(actorId));

        if (!isAdmin && !isCurrentDoctor && !isInitialDoctor && !isHandoverParticipant) {
            throw new VisitEncounterAccessDeniedException();
        }

        if (handovers.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> doctorIds = handovers.stream()
                .flatMap(h -> java.util.stream.Stream.of(h.getFromDoctorId(), h.getToDoctorId()))
                .collect(Collectors.toSet());

        Map<UUID, String> doctorNameMap = userRepository.findAllById(doctorIds.stream().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (existing, replacement) -> existing));

        return handovers.stream()
                .map(h -> new VisitHandoverResult(
                        h.getId(),
                        h.getVisitId(),
                        h.getFromDoctorId(),
                        doctorNameMap.getOrDefault(h.getFromDoctorId(), "Unknown"),
                        h.getToDoctorId(),
                        doctorNameMap.getOrDefault(h.getToDoctorId(), "Unknown"),
                        h.getReason(),
                        h.getHandedOverAt()
                ))
                .toList();
    }
}

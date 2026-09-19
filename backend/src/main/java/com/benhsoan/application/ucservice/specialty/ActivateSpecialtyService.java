package com.benhsoan.application.ucservice.specialty;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.inbound.specialty.ActivateSpecialtyUseCase;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivateSpecialtyService implements ActivateSpecialtyUseCase {

    private final SpecialtyRepository specialtyRepository;
    private final ClockPort clockPort;
    private final SpecialtyAuditService specialtyAuditService;

    @Override
    public SpecialtyResult activate(UUID id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new SpecialtyNotFoundException(id));

        Instant now = clockPort.now();
        specialty.activate(now);
        specialtyRepository.save(specialty);

        specialtyAuditService.record(ActionType.ACTIVATE, specialty);

        return new SpecialtyResult(
                specialty.getId(),
                specialty.getCode(),
                specialty.getName(),
                specialty.getDescription(),
                specialty.isActive()
        );
    }
}

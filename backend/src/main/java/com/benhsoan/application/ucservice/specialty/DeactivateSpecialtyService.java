package com.benhsoan.application.ucservice.specialty;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.CannotDeactivateDefaultSpecialtyException;
import com.benhsoan.domain.specialty.exception.SpecialtyInUseException;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.port.dto.command.specialty.DeactivateSpecialtyCommand;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.inbound.specialty.DeactivateSpecialtyUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.DoctorSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeactivateSpecialtyService implements DeactivateSpecialtyUseCase {

    private final SpecialtyRepository specialtyRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final MedicalRecordTemplateRepository medicalRecordTemplateRepository;
    private final ClockPort clockPort;
    private final SpecialtyAuditService specialtyAuditService;

    @Override
    public SpecialtyResult deactivate(DeactivateSpecialtyCommand command) {
        Specialty specialty = specialtyRepository.findById(command.specialtyId())
                .orElseThrow(() -> new SpecialtyNotFoundException(command.specialtyId()));

        if (Specialty.GENERAL_ID.equals(specialty.getId())) {
            throw new CannotDeactivateDefaultSpecialtyException();
        }

        long assignedDoctorCount = doctorSpecialtyRepository.countBySpecialtyId(specialty.getId());
        long activeTemplateCount = medicalRecordTemplateRepository.findBySpecialtyIdAndActive(specialty.getId(), true).size();

        if ((assignedDoctorCount > 0 || activeTemplateCount > 0) && !command.confirm()) {
            throw new SpecialtyInUseException(assignedDoctorCount, activeTemplateCount);
        }

        Instant now = clockPort.now();
        specialty.deactivate(now);
        specialtyRepository.save(specialty);

        specialtyAuditService.record(ActionType.DEACTIVATE, specialty);

        return new SpecialtyResult(
                specialty.getId(),
                specialty.getCode(),
                specialty.getName(),
                specialty.getDescription(),
                specialty.isActive()
        );
    }
}

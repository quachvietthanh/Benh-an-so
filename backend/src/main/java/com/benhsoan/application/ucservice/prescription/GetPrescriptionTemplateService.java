package com.benhsoan.application.ucservice.prescription;

import java.util.Objects;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.prescription.PrescriptionTemplate;
import com.benhsoan.domain.prescription.exception.PrescriptionTemplateNotFoundException;
import com.benhsoan.port.dto.result.PrescriptionTemplateResult;
import com.benhsoan.port.inbound.prescription.GetPrescriptionTemplateUseCase;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionTemplateRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/** NCL-05-CN-008: returns a single doctor-scoped template detail. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPrescriptionTemplateService implements GetPrescriptionTemplateUseCase {

    private final PrescriptionTemplateRepository templateRepository;
    private final PrescriptionTemplateResultMapper resultMapper;
    private final CurrentUserPort currentUserPort;

    @Override
    public PrescriptionTemplateResult getById(UUID templateId) {
        authorizeDoctor();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        PrescriptionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new PrescriptionTemplateNotFoundException(templateId));

        if (!Objects.equals(template.getCreatedBy(), currentUserId)) {
            throw new AccessDeniedException("Doctors may only view their own prescription templates.");
        }

        return resultMapper.toResult(template);
    }

    private void authorizeDoctor() {
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new AccessDeniedException("Only doctors are allowed to view prescription templates.");
        }
    }
}

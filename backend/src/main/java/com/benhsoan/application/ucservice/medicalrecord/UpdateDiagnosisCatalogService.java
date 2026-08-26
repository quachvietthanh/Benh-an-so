package com.benhsoan.application.ucservice.medicalrecord;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicalrecord.UpdateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.UpdateDiagnosisCatalogUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateDiagnosisCatalogService implements UpdateDiagnosisCatalogUseCase {

    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final ClockPort clockPort;
    private final DiagnosisCatalogResultMapper resultMapper;

    @Override
    public DiagnosisCatalogResult update(UpdateDiagnosisCatalogCommand command) {
        if (command == null || command.diagnosisCatalogId() == null) {
            throw new ValidationException("Diagnosis catalog id is required.");
        }

        DiagnosisCatalog catalog = diagnosisCatalogRepository.findById(command.diagnosisCatalogId())
                .orElseThrow(() -> new DiagnosisCatalogNotFoundException(command.diagnosisCatalogId()));
        catalog.updateInformation(command.name(), command.diseaseGroup(), command.description(), clockPort.now());
        return resultMapper.toResult(diagnosisCatalogRepository.save(catalog));
    }
}

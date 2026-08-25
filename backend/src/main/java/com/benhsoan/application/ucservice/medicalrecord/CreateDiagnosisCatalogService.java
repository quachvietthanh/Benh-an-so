package com.benhsoan.application.ucservice.medicalrecord;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogCodeAlreadyExistsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicalrecord.CreateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.CreateDiagnosisCatalogUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateDiagnosisCatalogService implements CreateDiagnosisCatalogUseCase {

    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final ClockPort clockPort;
    private final DiagnosisCatalogResultMapper resultMapper;

    @Override
    public DiagnosisCatalogResult create(CreateDiagnosisCatalogCommand command) {
        if (command == null) {
            throw new ValidationException("Create diagnosis catalog command is required.");
        }

        DiagnosisCatalog catalog = DiagnosisCatalog.create(
                UUID.randomUUID(), command.code(), command.name(), command.diseaseGroup(), command.description(), clockPort.now()
        );
        if (diagnosisCatalogRepository.existsByCode(catalog.getCode())) {
            throw new DiagnosisCatalogCodeAlreadyExistsException(catalog.getCode());
        }

        try {
            return resultMapper.toResult(diagnosisCatalogRepository.save(catalog));
        } catch (DataIntegrityViolationException exception) {
            throw new DiagnosisCatalogCodeAlreadyExistsException(catalog.getCode());
        }
    }
}

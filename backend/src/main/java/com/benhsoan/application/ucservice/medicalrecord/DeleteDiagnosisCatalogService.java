package com.benhsoan.application.ucservice.medicalrecord;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogDeletionNotAllowedException;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogInUseException;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.inbound.medicalrecord.DeleteDiagnosisCatalogUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeleteDiagnosisCatalogService implements DeleteDiagnosisCatalogUseCase {

    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;

    @Override
    public void delete(UUID diagnosisCatalogId) {
        if (diagnosisCatalogId == null) {
            throw new ValidationException("Diagnosis catalog id is required.");
        }
        if (diagnosisCatalogRepository.findById(diagnosisCatalogId).isEmpty()) {
            throw new DiagnosisCatalogNotFoundException(diagnosisCatalogId);
        }
        if (medicalRecordDiagnosisRepository.existsByDiagnosisCatalogId(diagnosisCatalogId)) {
            throw new DiagnosisCatalogInUseException(diagnosisCatalogId);
        }
        throw new DiagnosisCatalogDeletionNotAllowedException();
    }
}

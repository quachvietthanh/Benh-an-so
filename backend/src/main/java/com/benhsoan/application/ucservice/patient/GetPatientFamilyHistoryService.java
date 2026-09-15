package com.benhsoan.application.ucservice.patient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;
import com.benhsoan.port.inbound.patient.GetPatientFamilyHistoryUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientFamilyHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientFamilyHistoryService implements GetPatientFamilyHistoryUseCase {

    private final PatientRepository patientRepository;
    private final PatientFamilyHistoryRepository patientFamilyHistoryRepository;
    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final PatientFamilyHistoryResultMapper resultMapper;

    @Override
    public List<PatientFamilyHistoryResult> getFamilyHistory(UUID patientId) {
        if (patientId == null) {
            throw new ValidationException("Patient ID is required.");
        }
        if (patientRepository.findById(patientId).isEmpty()) {
            throw new PatientNotFoundException(patientId);
        }
        List<PatientFamilyHistory> histories = patientFamilyHistoryRepository.findByPatientIdAndActiveTrue(patientId);
        Map<UUID, DiagnosisCatalog> catalogs = loadCatalogs(
                histories.stream().map(PatientFamilyHistory::getDiagnosisCatalogId).toList());
        return histories.stream()
                .map(history -> toResult(history, catalogs.get(history.getDiagnosisCatalogId())))
                .toList();
    }

    private Map<UUID, DiagnosisCatalog> loadCatalogs(List<UUID> catalogIds) {
        if (catalogIds.isEmpty()) {
            return Map.of();
        }
        return diagnosisCatalogRepository.findAllByIds(catalogIds).stream()
                .collect(Collectors.toMap(DiagnosisCatalog::getId, Function.identity()));
    }

    private PatientFamilyHistoryResult toResult(PatientFamilyHistory history, DiagnosisCatalog catalog) {
        return resultMapper.toResult(history,
                catalog == null ? null : catalog.getCode(),
                catalog == null ? null : catalog.getName());
    }
}

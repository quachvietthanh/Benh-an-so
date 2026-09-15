package com.benhsoan.application.ucservice.patient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;
import com.benhsoan.port.inbound.patient.GetPatientChronicDiseasesUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChronicDiseaseRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientChronicDiseasesService implements GetPatientChronicDiseasesUseCase {

    private final PatientRepository patientRepository;
    private final PatientChronicDiseaseRepository patientChronicDiseaseRepository;
    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final PatientChronicDiseaseResultMapper resultMapper;

    @Override
    public List<PatientChronicDiseaseResult> getChronicDiseases(UUID patientId) {
        if (patientId == null) {
            throw new ValidationException("Patient ID is required.");
        }
        if (patientRepository.findById(patientId).isEmpty()) {
            throw new PatientNotFoundException(patientId);
        }
        List<PatientChronicDisease> diseases = patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId);
        Map<UUID, DiagnosisCatalog> catalogs = loadCatalogs(
                diseases.stream().map(PatientChronicDisease::getDiagnosisCatalogId).toList());
        return diseases.stream()
                .map(disease -> toResult(disease, catalogs.get(disease.getDiagnosisCatalogId())))
                .toList();
    }

    private Map<UUID, DiagnosisCatalog> loadCatalogs(List<UUID> catalogIds) {
        if (catalogIds.isEmpty()) {
            return Map.of();
        }
        return diagnosisCatalogRepository.findAllByIds(catalogIds).stream()
                .collect(Collectors.toMap(DiagnosisCatalog::getId, Function.identity()));
    }

    private PatientChronicDiseaseResult toResult(PatientChronicDisease disease, DiagnosisCatalog catalog) {
        return resultMapper.toResult(disease,
                catalog == null ? null : catalog.getCode(),
                catalog == null ? null : catalog.getName());
    }
}

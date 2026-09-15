package com.benhsoan.application.ucservice.patient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.VietnameseTextNormalizer;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.dto.result.patient.DuplicatePatientGroupResult;
import com.benhsoan.port.inbound.patient.FindDuplicatePatientsUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindDuplicatePatientsService implements FindDuplicatePatientsUseCase {

    private final PatientRepository patientRepository;
    private final PatientResultMapper patientResultMapper;

    private record GroupKey(String normalizedName, LocalDate dob, String phone) {}

    @Override
    public List<DuplicatePatientGroupResult> findDuplicates() {
        List<Patient> candidates = patientRepository.findSuspectedDuplicates();
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<GroupKey, List<PatientResult>> grouped = new LinkedHashMap<>();
        for (Patient p : candidates) {
            String normName = p.getFullName() == null ? "" : VietnameseTextNormalizer.normalize(p.getFullName());
            String phone = p.getPhone() == null ? "" : p.getPhone().trim().replaceAll("\\s+", "");
            GroupKey key = new GroupKey(normName, p.getDateOfBirth(), phone);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(patientResultMapper.toResult(p));
        }

        List<DuplicatePatientGroupResult> results = new ArrayList<>();
        for (Map.Entry<GroupKey, List<PatientResult>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                PatientResult first = entry.getValue().get(0);
                results.add(new DuplicatePatientGroupResult(
                        first.fullName(),
                        first.dateOfBirth(),
                        first.phone(),
                        entry.getValue()
                ));
            }
        }
        return results;
    }
}

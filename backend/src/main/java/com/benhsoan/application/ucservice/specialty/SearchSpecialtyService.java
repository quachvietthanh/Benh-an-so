package com.benhsoan.application.ucservice.specialty;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordTemplateResultMapper;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.inbound.specialty.SearchSpecialtyUseCase;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchSpecialtyService implements SearchSpecialtyUseCase {

    private final SpecialtyRepository specialtyRepository;
    private final MedicalRecordTemplateResultMapper resultMapper;

    @Override
    public List<SpecialtyResult> search(Boolean active) {
        return specialtyRepository.findByActive(active == null || active).stream().map(resultMapper::toResult).toList();
    }
}

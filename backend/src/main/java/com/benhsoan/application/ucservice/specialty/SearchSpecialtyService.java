package com.benhsoan.application.ucservice.specialty;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.inbound.specialty.SearchSpecialtyUseCase;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchSpecialtyService implements SearchSpecialtyUseCase {

    private final SpecialtyRepository specialtyRepository;

    @Override
    public List<SpecialtyResult> search(String keyword, Boolean active) {
        List<Specialty> specialties;
        if (keyword != null && !keyword.isBlank()) {
            specialties = specialtyRepository.search(keyword.trim(), active);
        } else if (active != null) {
            specialties = specialtyRepository.findByActive(active);
        } else {
            specialties = specialtyRepository.findAll();
        }
        return specialties.stream()
                .map(this::toResult)
                .toList();
    }

    private SpecialtyResult toResult(Specialty specialty) {
        return new SpecialtyResult(
                specialty.getId(),
                specialty.getCode(),
                specialty.getName(),
                specialty.getDescription(),
                specialty.isActive()
        );
    }
}

package com.benhsoan.application.ucservice.medicine;

import org.springframework.data.domain.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicine.SearchMedicinesQuery;
import com.benhsoan.port.dto.result.MedicineResult;
import com.benhsoan.port.inbound.medicine.SearchMedicinesUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineSearchCriteria;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchMedicinesService implements SearchMedicinesUseCase {

    private final MedicineRepository medicineRepository;

    private final MedicineManagementAuthorizer authorizer;

    private final MedicineResultMapper resultMapper;

    @Override
    public Page<MedicineResult> search(SearchMedicinesQuery query) {
        validateQuery(query);
        authorizer.requireReadAccess();

        MedicineSearchCriteria criteria = new MedicineSearchCriteria(
                query.keyword(),
                null,
                null,
                query.active()
        );
        return medicineRepository.search(criteria, query.pageable())
                .map(resultMapper::toResult);
    }

    private static void validateQuery(SearchMedicinesQuery query) {
        if (query == null) {
            throw new ValidationException("Search medicines query is required.");
        }
        if (query.pageable() == null) {
            throw new ValidationException("Medicine pageable is required.");
        }
    }
}

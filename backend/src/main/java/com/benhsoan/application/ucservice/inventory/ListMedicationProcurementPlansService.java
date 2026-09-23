package com.benhsoan.application.ucservice.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.port.dto.query.inventory.ListProcurementPlansQuery;
import com.benhsoan.port.dto.result.ProcurementPlanSummaryResult;
import com.benhsoan.port.inbound.inventory.ListMedicationProcurementPlansUseCase;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanSearchCriteria;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListMedicationProcurementPlansService implements ListMedicationProcurementPlansUseCase {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final MedicationProcurementPlanRepository planRepository;
    private final MedicationProcurementResultMapper resultMapper;

    @Override
    public Page<ProcurementPlanSummaryResult> list(ListProcurementPlansQuery query) {
        int page = (query != null && query.page() >= 0) ? query.page() : DEFAULT_PAGE;
        int size = (query != null && query.size() > 0) ? Math.min(query.size(), MAX_SIZE) : DEFAULT_SIZE;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        MedicationProcurementPlanSearchCriteria criteria = null;
        if (query != null) {
            criteria = new MedicationProcurementPlanSearchCriteria(
                    query.status(),
                    query.fromDate(),
                    query.toDate(),
                    query.createdBy()
            );
        }

        Page<MedicationProcurementPlan> planPage = planRepository.findAll(criteria, pageable);
        return planPage.map(resultMapper::toSummaryResult);
    }
}

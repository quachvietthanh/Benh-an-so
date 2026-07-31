package com.benhsoan.application.ucservice.clinical;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.clinical.GetClinicalOrdersByVisitQuery;
import com.benhsoan.port.dto.result.ClinicalOrderResult;
import com.benhsoan.port.inbound.clinical.GetClinicalOrdersByVisitUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetClinicalOrdersByVisitService implements GetClinicalOrdersByVisitUseCase {

    private final VisitRepository visitRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ClinicalOrderAuthorizationService authorizationService;
    private final ClinicalOrderResultMapper resultMapper;

    @Override
    public Page<ClinicalOrderResult> getOrdersByVisit(GetClinicalOrdersByVisitQuery query) {
        authorizationService.requireReadAccess();
        visitRepository.findById(query.visitId()).orElseThrow(() -> new VisitNotFoundException(query.visitId()));
        var orders = clinicalOrderRepository.findByVisitId(query.visitId(), PageRequest.of(query.page(), query.size()));
        List<UUID> orderIds = orders.getContent().stream().map(order -> order.getId()).toList();
        Map<UUID, List<ClinicalOrderItem>> itemsByOrderId = clinicalOrderItemRepository.findByClinicalOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(ClinicalOrderItem::getClinicalOrderId));
        return orders.map(order -> resultMapper.toResult(order, itemsByOrderId.getOrDefault(order.getId(), List.of())));
    }
}

package com.benhsoan.application.ucservice.clinical;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.clinical.GetPendingClinicalOrdersQuery;
import com.benhsoan.port.dto.result.PendingClinicalOrderResult;
import com.benhsoan.port.inbound.clinical.GetPendingClinicalOrdersUseCase;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPendingClinicalOrdersService implements GetPendingClinicalOrdersUseCase {

    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ClinicalOrderAuthorizationService authorizationService;
    private final ClinicalOrderAuditService auditService;
    private final ClockPort clockPort;

    @Override
    public Page<PendingClinicalOrderResult> getPendingOrders(GetPendingClinicalOrdersQuery query) {
        UUID actorId = authorizationService.requireReadAccess();
        Instant now = clockPort.now();

        UUID effectiveDoctorId = authorizationService.isAdmin() ? query.doctorId() : actorId;

        Page<PendingClinicalOrderResult> page = clinicalOrderItemRepository.findPendingOrders(
                query.patientId(),
                effectiveDoctorId,
                query.fromDate(),
                query.toDate(),
                now,
                PageRequest.of(query.page(), query.size())
        );

        auditService.recordViewPending(actorId, now);

        return page;
    }
}

package com.benhsoan.application.ucservice.prescription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.prescription.SearchPrescriptionsQuery;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.SearchPrescriptionsUseCase;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchPrescriptionsService implements SearchPrescriptionsUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionWarningLogRepository warningLogRepository;
    private final PrescriptionReadAccessValidator accessValidator;
    private final PrescriptionResultMapper resultMapper;

    @Override
    public Page<PrescriptionResult> search(SearchPrescriptionsQuery query) {
        accessValidator.requireCanReadDispensingQueue();

        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.ASC, "prescribedAt")
        );

        return prescriptionRepository.findByStatus(query.status(), pageable)
                .map(prescription -> resultMapper.toResult(
                        prescription,
                        warningLogRepository.findByPrescriptionId(prescription.getId())
                ));
    }
}

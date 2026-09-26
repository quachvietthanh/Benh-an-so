package com.benhsoan.application.ucservice.clinic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.clinic.ClinicConfigurationResult;
import com.benhsoan.port.inbound.clinic.GetClinicConfigurationUseCase;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetClinicConfigurationService implements GetClinicConfigurationUseCase {

    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final ClinicConfigurationResultMapper resultMapper;

    @Override
    public ClinicConfigurationResult get() {
        return clinicConfigurationRepository.find()
                .map(resultMapper::toResult)
                .orElseGet(ClinicConfigurationResult::empty);
    }
}

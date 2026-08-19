package com.benhsoan.application.ucservice.clinic;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinic.UpdateClinicConfigurationCommand;
import com.benhsoan.port.dto.result.clinic.ClinicConfigurationResult;
import com.benhsoan.port.inbound.clinic.UpdateClinicConfigurationUseCase;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateClinicConfigurationService implements UpdateClinicConfigurationUseCase {

    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final ClockPort clockPort;
    private final ClinicConfigurationResultMapper resultMapper;

    @Override
    public ClinicConfigurationResult update(UpdateClinicConfigurationCommand command) {
        if (command == null) {
            throw new ValidationException("Update clinic configuration command is required.");
        }

        Instant now = clockPort.now();
        ClinicConfiguration configuration = clinicConfigurationRepository.find()
                .map(existing -> update(existing, command, now))
                .orElseGet(() -> ClinicConfiguration.create(
                        command.clinicName(),
                        command.address(),
                        command.phone(),
                        command.openingTime(),
                        command.closingTime(),
                        now
                ));

        return resultMapper.toResult(clinicConfigurationRepository.save(configuration));
    }

    private static ClinicConfiguration update(
            ClinicConfiguration configuration,
            UpdateClinicConfigurationCommand command,
            Instant updatedAt
    ) {
        configuration.update(
                command.clinicName(),
                command.address(),
                command.phone(),
                command.openingTime(),
                command.closingTime(),
                updatedAt
        );
        return configuration;
    }
}

package com.benhsoan.application.ucservice.clinic;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.port.dto.result.clinic.ClinicConfigurationResult;

@Component
class ClinicConfigurationResultMapper {

    ClinicConfigurationResult toResult(ClinicConfiguration configuration) {
        return new ClinicConfigurationResult(
                configuration.getClinicName(),
                configuration.getAddress(),
                configuration.getPhone(),
                configuration.getOpeningTime(),
                configuration.getClosingTime(),
                configuration.getRetentionYears()
        );
    }
}

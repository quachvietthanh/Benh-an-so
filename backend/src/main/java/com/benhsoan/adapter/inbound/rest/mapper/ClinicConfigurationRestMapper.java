package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.clinic.UpdateClinicConfigurationRequest;
import com.benhsoan.adapter.inbound.rest.response.clinic.ClinicConfigurationResponse;
import com.benhsoan.port.dto.command.clinic.UpdateClinicConfigurationCommand;
import com.benhsoan.port.dto.result.clinic.ClinicConfigurationResult;

@Component
public class ClinicConfigurationRestMapper {

    public UpdateClinicConfigurationCommand toCommand(UpdateClinicConfigurationRequest request) {
        return new UpdateClinicConfigurationCommand(
                request.clinicName(),
                request.address(),
                request.phone(),
                request.openingTime(),
                request.closingTime(),
                request.retentionYears()
        );
    }

    public ClinicConfigurationResponse toResponse(ClinicConfigurationResult result) {
        return new ClinicConfigurationResponse(
                result.clinicName(),
                result.address(),
                result.phone(),
                result.openingTime(),
                result.closingTime(),
                result.retentionYears()
        );
    }
}

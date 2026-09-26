package com.benhsoan.port.inbound.clinic;

import com.benhsoan.port.dto.command.clinic.UpdateClinicConfigurationCommand;
import com.benhsoan.port.dto.result.clinic.ClinicConfigurationResult;

public interface UpdateClinicConfigurationUseCase {

    ClinicConfigurationResult update(UpdateClinicConfigurationCommand command);
}

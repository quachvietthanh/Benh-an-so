package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.PatientPortalRegistrationCommand;
import com.benhsoan.port.dto.result.PatientPortalRegistrationResult;

public interface PatientPortalRegistrationUseCase {

    PatientPortalRegistrationResult register(PatientPortalRegistrationCommand command);

}

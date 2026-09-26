package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.PatientLoginCommand;
import com.benhsoan.port.dto.result.PatientLoginResult;

public interface PatientLoginUseCase {

    PatientLoginResult login(PatientLoginCommand command);

}

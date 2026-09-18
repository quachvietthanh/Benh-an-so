package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.PatientResetPasswordCommand;
import com.benhsoan.port.dto.result.PatientResetPasswordResult;

public interface PatientResetPasswordUseCase {

    PatientResetPasswordResult resetPassword(PatientResetPasswordCommand command);
}

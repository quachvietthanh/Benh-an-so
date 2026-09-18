package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.PatientForgotPasswordCommand;
import com.benhsoan.port.dto.result.PatientForgotPasswordResult;

public interface PatientForgotPasswordUseCase {

    PatientForgotPasswordResult forgotPassword(PatientForgotPasswordCommand command);
}

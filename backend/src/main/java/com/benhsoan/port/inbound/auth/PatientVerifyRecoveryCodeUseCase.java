package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.PatientVerifyRecoveryCodeCommand;

public interface PatientVerifyRecoveryCodeUseCase {

    void verifyCode(PatientVerifyRecoveryCodeCommand command);
}

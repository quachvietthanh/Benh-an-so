package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.VerifyTwoFactorCommand;
import com.benhsoan.port.dto.result.LoginResult;

public interface TwoFactorVerificationUseCase {

    LoginResult verify(VerifyTwoFactorCommand command);
}

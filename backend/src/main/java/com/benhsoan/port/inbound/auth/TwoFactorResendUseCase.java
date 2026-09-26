package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.ResendTwoFactorCommand;
import com.benhsoan.port.dto.result.TwoFactorResendResult;

public interface TwoFactorResendUseCase {

    TwoFactorResendResult resend(ResendTwoFactorCommand command);
}

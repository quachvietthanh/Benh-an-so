package com.benhsoan.port.inbound.user;

import com.benhsoan.port.dto.command.user.ResetPasswordCommand;
import com.benhsoan.port.dto.result.ResetPasswordResult;

public interface ResetPasswordUseCase {

    ResetPasswordResult resetPassword(ResetPasswordCommand command);
}

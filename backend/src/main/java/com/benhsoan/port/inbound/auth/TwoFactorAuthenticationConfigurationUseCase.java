package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.ConfigureTwoFactorAuthenticationCommand;
import com.benhsoan.port.dto.result.TwoFactorConfigurationResult;

public interface TwoFactorAuthenticationConfigurationUseCase {

    TwoFactorConfigurationResult configure(ConfigureTwoFactorAuthenticationCommand command);
}

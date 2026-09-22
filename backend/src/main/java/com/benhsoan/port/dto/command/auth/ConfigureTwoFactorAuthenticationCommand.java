package com.benhsoan.port.dto.command.auth;

public record ConfigureTwoFactorAuthenticationCommand(

        String roleName,

        boolean enabled

) {}

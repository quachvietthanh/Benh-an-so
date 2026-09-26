package com.benhsoan.adapter.inbound.rest.request.auth;

import jakarta.validation.constraints.NotNull;

public record ConfigureTwoFactorAuthenticationRequest(

        @NotNull
        Boolean enabled

) {}

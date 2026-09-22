package com.benhsoan.adapter.inbound.rest.request.auth;

import jakarta.validation.constraints.NotBlank;

public record ResendTwoFactorRequest(

        @NotBlank
        String twoFactorToken

) {}

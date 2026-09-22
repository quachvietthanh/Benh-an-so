package com.benhsoan.adapter.inbound.rest.request.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyTwoFactorRequest(

        @NotBlank
        String twoFactorToken,

        @NotBlank
        String code

) {}

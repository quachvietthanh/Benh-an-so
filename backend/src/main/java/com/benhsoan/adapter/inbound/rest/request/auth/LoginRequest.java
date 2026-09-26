package com.benhsoan.adapter.inbound.rest.request.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank
        String username,

        @NotBlank
        String password

) {
}

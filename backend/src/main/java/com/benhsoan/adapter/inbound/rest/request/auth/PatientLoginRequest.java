package com.benhsoan.adapter.inbound.rest.request.auth;

import jakarta.validation.constraints.NotBlank;

public record PatientLoginRequest(

        @NotBlank
        String phone,

        @NotBlank
        String password

) {
}

package com.benhsoan.adapter.inbound.rest.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(

        @NotBlank
        String username,

        @NotBlank
        String password,

        @NotBlank
        String fullName,

        @Email
        String email,

        String phone,

        @NotBlank
        String roleName

) {
}
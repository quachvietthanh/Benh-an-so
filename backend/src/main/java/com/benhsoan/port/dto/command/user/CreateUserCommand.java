package com.benhsoan.port.dto.command.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserCommand(

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
package com.benhsoan.port.dto.command.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserCommand(

        @NotBlank
        @Size(max = 100)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 100)
        String email,

        @Size(max = 20)
        String phone,

        @NotBlank
        String roleName

) {
}
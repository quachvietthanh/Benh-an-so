package com.benhsoan.port.dto.command.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand(

        @NotBlank
        String username,

        @NotBlank
        String password

){}

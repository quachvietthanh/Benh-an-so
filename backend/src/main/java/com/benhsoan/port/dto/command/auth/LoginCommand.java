package com.benhsoan.port.dto.command.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand(

        @NotBlank
        String username,

        @NotBlank
        String password,

        String ipAddress,

        String userAgent

) {
    public LoginCommand(String username, String password) {
        this(username, password, null, null);
    }
}

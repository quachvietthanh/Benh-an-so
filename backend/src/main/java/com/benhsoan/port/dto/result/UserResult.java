package com.benhsoan.port.dto.result;

import java.util.UUID;

public record UserResult(

        UUID id,

        String username,

        String fullName,

        String email,

        String phone,

        String roleName,

        boolean active

) {
}
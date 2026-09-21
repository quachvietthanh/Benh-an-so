package com.benhsoan.port.dto.result;

import java.util.UUID;

public record SpecialtyResult(UUID id, String code, String name, String description, boolean active) {

    public SpecialtyResult(UUID id, String code, String name, boolean active) {
        this(id, code, name, null, active);
    }
}

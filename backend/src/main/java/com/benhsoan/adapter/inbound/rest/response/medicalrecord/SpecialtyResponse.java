package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.util.UUID;

public record SpecialtyResponse(UUID id, String code, String name, String description, boolean active) {

    public SpecialtyResponse(UUID id, String code, String name, boolean active) {
        this(id, code, name, null, active);
    }
}

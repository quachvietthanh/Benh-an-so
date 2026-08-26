package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.util.UUID;

public record SpecialtyResponse(UUID id, String code, String name, boolean active) {
}

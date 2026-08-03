package com.benhsoan.adapter.inbound.rest.request.queue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
        @NotBlank @Size(max = 100) String name
) {
}

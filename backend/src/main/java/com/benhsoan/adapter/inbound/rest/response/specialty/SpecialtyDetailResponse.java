package com.benhsoan.adapter.inbound.rest.response.specialty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.adapter.inbound.rest.response.queue.RoomResponse;

import lombok.Builder;

@Builder
public record SpecialtyDetailResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        List<AssignedDoctorResponse> doctors,
        List<RoomResponse> rooms,
        long activeTemplateCount,
        Instant createdAt,
        Instant updatedAt
) {

    @Builder
    public record AssignedDoctorResponse(
            UUID id,
            String username,
            String fullName,
            String email,
            String phone
    ) {
    }
}

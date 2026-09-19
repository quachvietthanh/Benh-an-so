package com.benhsoan.port.dto.result.specialty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.RoomResult;

import lombok.Builder;

@Builder
public record SpecialtyDetailResult(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        List<AssignedDoctorInfo> doctors,
        List<RoomResult> rooms,
        long activeTemplateCount,
        Instant createdAt,
        Instant updatedAt
) {

    @Builder
    public record AssignedDoctorInfo(
            UUID id,
            String username,
            String fullName,
            String email,
            String phone
    ) {
    }
}

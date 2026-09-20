package com.benhsoan.adapter.inbound.rest.request.specialty;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSpecialtyRequest(
        @NotBlank(message = "Tên chuyên khoa không được để trống.")
        @Size(max = 100, message = "Tên chuyên khoa không được vượt quá 100 ký tự.")
        String name,

        @Size(max = 500, message = "Mô tả chuyên khoa không được vượt quá 500 ký tự.")
        String description,

        List<UUID> doctorIds,

        List<UUID> roomIds
) {
}

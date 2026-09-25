package com.benhsoan.adapter.inbound.rest.request.backup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateBackupScheduleRequest(
        @NotNull(message = "Trạng thái kích hoạt lịch không được để trống.")
        Boolean enabled,

        @NotBlank(message = "Thời gian sao lưu hàng ngày không được để trống.")
        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Thời gian sao lưu hàng ngày phải có định dạng HH:mm (00:00 - 23:59).")
        String dailyTime
) {
}

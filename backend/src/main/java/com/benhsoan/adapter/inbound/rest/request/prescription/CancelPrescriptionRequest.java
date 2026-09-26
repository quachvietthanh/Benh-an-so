package com.benhsoan.adapter.inbound.rest.request.prescription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu hủy đơn thuốc")
public record CancelPrescriptionRequest(
        @Schema(description = "Lý do hủy đơn thuốc", example = "Bệnh nhân thay đổi phương án điều trị", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Lý do hủy đơn thuốc là bắt buộc.")
        @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự.")
        String cancelReason
) {
}

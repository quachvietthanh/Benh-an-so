package com.benhsoan.adapter.inbound.rest.request.billing;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectDiscountRequest {

    @NotBlank(message = "Lý do từ chối không được để trống.")
    private String rejectionReason;
}

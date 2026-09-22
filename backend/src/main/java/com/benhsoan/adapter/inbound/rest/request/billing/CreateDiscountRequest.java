package com.benhsoan.adapter.inbound.rest.request.billing;

import java.math.BigDecimal;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.DiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateDiscountRequest {

    @NotNull(message = "Mã lượt khám không được để trống.")
    private UUID visitId;

    @NotNull(message = "Loại giảm giá không được để trống.")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống.")
    @DecimalMin(value = "0.00", message = "Giá trị giảm giá không được nhỏ hơn 0.")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", message = "Tổng tiền gốc ban đầu không được nhỏ hơn 0.")
    private BigDecimal originalAmount;

    @NotBlank(message = "Lý do đề nghị giảm giá không được để trống.")
    private String reason;
}

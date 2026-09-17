package com.benhsoan.port.dto.result;

import java.math.BigDecimal;

public record ServiceGroupRevenueResult(
        String groupCode,
        String groupName,
        BigDecimal revenue,
        BigDecimal percentage
) {
}

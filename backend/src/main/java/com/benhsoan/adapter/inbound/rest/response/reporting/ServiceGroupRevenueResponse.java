package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.math.BigDecimal;

public record ServiceGroupRevenueResponse(
        String groupCode,
        String groupName,
        BigDecimal revenue,
        BigDecimal percentage
) {
}

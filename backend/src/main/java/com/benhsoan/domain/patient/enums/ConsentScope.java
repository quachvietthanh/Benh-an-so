package com.benhsoan.domain.patient.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum ConsentScope {
    TREATMENT("Khám chữa bệnh và lưu trữ bệnh án"),
    COMMUNICATION("Thông báo nhắc lịch hẹn và chăm sóc khách hàng"),
    RESEARCH("Nghiên cứu khoa học và đào tạo y khoa nội bộ");

    private final String description;

    ConsentScope(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static Set<ConsentScope> defaultAll() {
        return EnumSet.allOf(ConsentScope.class);
    }

    public static Set<ConsentScope> parse(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return ConsentScope.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ConsentScope.class)));
    }

    public static String toCommaSeparated(Set<ConsentScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "";
        }
        return scopes.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}

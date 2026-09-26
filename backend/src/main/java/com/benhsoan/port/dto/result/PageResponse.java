package com.benhsoan.port.dto.result;

import java.util.List;

public record PageResponse<T>(

        List<T> content,

        int page,

        int size,

        long totalElements,

        int totalPages

) {
    public static <T> PageResponse<T> of(
            List<T> content,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = size > 0
                ? (int) Math.ceil((double) totalElements / size)
                : 0;
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0, 0);
    }
}

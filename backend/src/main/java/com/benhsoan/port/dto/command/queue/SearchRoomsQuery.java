package com.benhsoan.port.dto.command.queue;

import com.benhsoan.domain.shared.exception.ValidationException;

public record SearchRoomsQuery(String keyword, Boolean active, int page, int size) {

    public SearchRoomsQuery {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
    }
}

package com.benhsoan.domain.shared.exception;


import lombok.Getter;

@Getter
public abstract class DomainException extends RuntimeException {

    private final DomainErrorCode code;

    protected DomainException(
            String message
    ) {
        super(message);
        this.code = DomainErrorCode.forException(getClass().asSubclass(DomainException.class));
    }

}

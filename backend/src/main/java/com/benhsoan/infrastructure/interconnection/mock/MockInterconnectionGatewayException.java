package com.benhsoan.infrastructure.interconnection.mock;

import org.springframework.http.HttpStatus;

public class MockInterconnectionGatewayException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public MockInterconnectionGatewayException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}

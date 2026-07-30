package com.benhsoan.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsNotFoundForMissingRouteInsteadOfInternalServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/api/v1/medical-records");

        var response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/medical-records"), request
        );

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Resource not found.", response.getBody().message());
    }
}

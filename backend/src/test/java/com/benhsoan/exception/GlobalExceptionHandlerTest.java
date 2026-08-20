package com.benhsoan.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.benhsoan.domain.prescription.exception.PrescriptionInsufficientStockException;
import com.benhsoan.domain.reporting.exception.OperationalReportDataEmptyException;

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

    @Test
    void returnsStructuredConflictForInsufficientStock() {
        UUID prescriptionId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest(
                HttpMethod.POST.name(),
                "/prescriptions/" + prescriptionId + "/dispense"
        );

        var response = handler.handleInsufficientStock(
                new PrescriptionInsufficientStockException(
                        prescriptionId,
                        List.of(new PrescriptionInsufficientStockException.StockShortageDetail(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "MED-001",
                                "Paracetamol",
                                20,
                                5,
                                15
                        ))
                ),
                request
        );

        assertEquals(409, response.getStatusCode().value());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("INSUFFICIENT_STOCK", response.getBody().code());
        assertEquals(request.getRequestURI(), response.getBody().path());
        assertEquals(prescriptionId, response.getBody().prescriptionId());
        assertEquals(1, response.getBody().details().size());
        assertEquals(15, response.getBody().details().get(0).shortageQuantity());
    }

    @Test
    void returnsStructuredUnprocessableContentForEmptyOperationalReport() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/reports/export");

        var response = handler.handleOperationalReportDataEmpty(
                new OperationalReportDataEmptyException(), request
        );

        assertEquals(422, response.getStatusCode().value());
        assertEquals("REPORT_DATA_EMPTY", response.getBody().code());
        assertEquals("No report data available for the selected period.", response.getBody().message());
        assertEquals(request.getRequestURI(), response.getBody().path());
    }
}

package com.benhsoan.exception;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.prescription.exception.PrescriptionInsufficientStockException;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException;
import com.benhsoan.domain.auth.exception.InvalidCredentialsException;
import com.benhsoan.domain.queue.exception.DoctorNotAssignedToRoomException;
import com.benhsoan.domain.reporting.exception.OperationalReportDataEmptyException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.pdf.PdfRenderingException;
import com.fasterxml.jackson.databind.JsonMappingException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest(HttpMethod.POST.name(), "/api/test-resource");
    }

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
        assertEquals(prescriptionId, response.getBody().details().get("prescriptionId"));
        var shortages = (List<?>) response.getBody().details().get("shortages");
        assertEquals(1, shortages.size());
        assertEquals(15, ((java.util.Map<?, ?>) shortages.getFirst()).get("shortageQuantity"));
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

    @Test
    void returnsTheExplicitStableCodeForDomainExceptions() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST.name(), "/patients");

        var response = handler.handleDomainException(new ValidationException("Patient name is required."), request);

        assertEquals("VALIDATION_FAILED", response.getBody().code());
    }

    @Test
    void mapsAuthenticationAndBusinessPreconditionExceptionsToTheirSemanticStatuses() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST.name(), "/auth/login");

        var invalidCredentials = handler.handleDomainException(new InvalidCredentialsException(), request);
        var doctorNotAssigned = handler.handleDomainException(
                new DoctorNotAssignedToRoomException(UUID.randomUUID()), request);

        assertEquals(401, invalidCredentials.getStatusCode().value());
        assertEquals("INVALID_CREDENTIALS", invalidCredentials.getBody().code());
        assertEquals(409, doctorNotAssigned.getStatusCode().value());
        assertEquals("DOCTOR_NOT_ASSIGNED_TO_ROOM", doctorNotAssigned.getBody().code());
    }

    @Test
    void returnsCommonContractForBeanValidationErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "patient.name", "Patient name is required."));
        bindingResult.addError(new FieldError("request", "quantity", "Quantity must be positive."));

        var body = assertContract(handler.handleValidation(
                new MethodArgumentNotValidException(null, bindingResult), request), 400, "VALIDATION_FAILED");

        assertEquals(Map.of(
                "patient.name", "Patient name is required.",
                "quantity", "Quantity must be positive."), body.details().get("fields"));
    }

    @Test
    void returnsNestedFieldPathForJsonTypeErrors() {
        JsonMappingException mappingException = JsonMappingException.from(
                (com.fasterxml.jackson.core.JsonParser) null, "Invalid numeric value.");
        mappingException.prependPath(new Object(), "dosage");
        mappingException.prependPath(new Object(), 2);
        mappingException.prependPath(new Object(), "items");

        var body = assertContract(handler.handleUnreadable(
                new HttpMessageNotReadableException("Unreadable JSON", mappingException), request),
                400, "VALIDATION_FAILED");

        assertEquals(Map.of("items[2].dosage", "Invalid value."), body.details().get("fields"));
    }

    @Test
    void returnsCommonContractForAuthenticationAndAuthorization() {
        var authentication = assertContract(handler.handleAuthentication(
                new AuthenticationException("Token rejected") { }, request), 401, "AUTHENTICATION_FAILED");
        var authorization = assertContract(handler.handleAccessDenied(
                new org.springframework.security.access.AccessDeniedException("Denied"), request),
                403, "ACCESS_DENIED");

        assertEquals("Token rejected", authentication.message());
        assertEquals("Access denied.", authorization.message());
    }

    @Test
    void returnsCommonContractForUploadErrors() {
        var missingPart = assertContract(handler.handleMissingRequestPart(
                new MissingServletRequestPartException("file"), request), 400, "MISSING_REQUEST_PART");
        var tooLarge = assertContract(handler.handleUploadTooLarge(
                new MaxUploadSizeExceededException(1_024), request), 413, "PAYLOAD_TOO_LARGE");

        assertEquals("file is required.", missingPart.message());
        assertEquals("Uploaded file exceeds the allowed size.", tooLarge.message());
    }

    @Test
    void hidesInfrastructureCausesBehindTheCommonInternalErrorContract() {
        var body = assertContract(handler.handleUnknown(new PdfRenderingException(
                "Unable to generate prescription PDF.", new IOException("disk path: /secret/output")), request),
                500, "INTERNAL_SERVER_ERROR");

        assertEquals("Internal server error.", body.message());
        assertFalse(body.message().contains("secret"));
        assertFalse(body.details().containsKey("cause"));
    }

    @Test
    void preservesCodeAndStructuredDetailsForSpecializedBusinessErrors() {
        UUID prescriptionId = UUID.randomUUID();
        var stock = assertContract(handler.handleInsufficientStock(new PrescriptionInsufficientStockException(
                prescriptionId,
                List.of(new PrescriptionInsufficientStockException.StockShortageDetail(
                        UUID.randomUUID(), UUID.randomUUID(), "MED-001", "Paracetamol", 10, 3, 7))), request),
                409, "INSUFFICIENT_STOCK");
        var interaction = assertContract(handler.handleInteractionConfirmationRequired(
                new PrescriptionInteractionConfirmationRequiredException(List.of(
                        new PrescriptionInteractionConfirmationRequiredException.InteractionWarning(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                                InteractionSeverity.SEVERE, "Interaction", "Confirm override"))), request),
                409, "INTERACTION_CONFIRMATION_REQUIRED");

        assertEquals(prescriptionId, stock.details().get("prescriptionId"));
        assertEquals(7, ((Map<?, ?>) ((List<?>) stock.details().get("shortages")).getFirst()).get("shortageQuantity"));
        assertEquals(InteractionSeverity.SEVERE,
                ((Map<?, ?>) ((List<?>) interaction.details().get("warnings")).getFirst()).get("severity"));
    }

    private ApiErrorResponse assertContract(
            ResponseEntity<ApiErrorResponse> response,
            int expectedStatus,
            String expectedCode
    ) {
        ApiErrorResponse body = response.getBody();
        assertAll(
                () -> assertEquals(expectedStatus, response.getStatusCode().value()),
                () -> assertNotNull(body),
                () -> assertNotNull(body.timestamp()),
                () -> assertEquals(expectedStatus, body.status()),
                () -> assertNotNull(body.error()),
                () -> assertEquals(expectedCode, body.code()),
                () -> assertNotNull(body.message()),
                () -> assertEquals(request.getRequestURI(), body.path()),
                () -> assertNotNull(body.details())
        );
        return body;
    }
}

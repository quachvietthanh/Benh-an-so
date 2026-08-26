package com.benhsoan.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.benhsoan.domain.auth.exception.TooManyLoginAttemptsException;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException;
import com.benhsoan.domain.prescription.exception.PrescriptionInsufficientStockException;
import com.benhsoan.domain.reporting.exception.OperationalReportDataEmptyException;
import com.benhsoan.domain.shared.exception.DomainException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OperationalReportDataEmptyException.class)
    public ResponseEntity<ApiErrorResponse> handleOperationalReportDataEmpty(
            OperationalReportDataEmptyException ex,
            HttpServletRequest request
    ) {
        return build(DomainExceptionHttpStatusMapper.statusFor(ex.getCode()), ex.getCode().name(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(
            DomainException ex,
            HttpServletRequest request
    ) {

        return build(DomainExceptionHttpStatusMapper.statusFor(ex.getCode()), ex.getCode().name(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ApiErrorResponse> handleTooManyLoginAttempts(
            TooManyLoginAttemptsException ex,
            HttpServletRequest request
    ) {

        HttpStatus status = DomainExceptionHttpStatusMapper.statusFor(ex.getCode());
        long retryAfterSeconds = Math.max(0, ex.getRetryAfterSeconds());

        return ResponseEntity
                .status(status)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds))
                .body(ApiErrorResponseFactory.create(
                        status,
                        ex.getCode().name(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        Map.of("retryAfterSeconds", retryAfterSeconds)));
    }

    @ExceptionHandler(PrescriptionInteractionConfirmationRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleInteractionConfirmationRequired(
            PrescriptionInteractionConfirmationRequiredException ex,
            HttpServletRequest request
    ) {
        return build(
                DomainExceptionHttpStatusMapper.statusFor(ex.getCode()),
                ex.getCode().name(),
                ex.getMessage(),
                request.getRequestURI(),
                Map.of("warnings", ex.getWarnings().stream().map(warning -> Map.<String, Object>of(
                        "ruleId", warning.ruleId(),
                        "firstMedicineId", warning.firstMedicineId(),
                        "secondMedicineId", warning.secondMedicineId(),
                        "severity", warning.severity(),
                        "description", warning.description(),
                        "recommendation", warning.recommendation()
                )).toList())
        );
    }

    @ExceptionHandler(PrescriptionInsufficientStockException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientStock(
            PrescriptionInsufficientStockException ex,
            HttpServletRequest request
    ) {
        return build(
                DomainExceptionHttpStatusMapper.statusFor(ex.getCode()),
                ex.getCode().name(),
                ex.getMessage(),
                request.getRequestURI(),
                Map.of(
                        "prescriptionId", ex.getPrescriptionId(),
                        "shortages", ex.getDetails().stream().map(shortage -> Map.<String, Object>of(
                                "prescriptionItemId", shortage.prescriptionItemId(),
                                "medicineId", shortage.medicineId(),
                                "medicineCode", shortage.medicineCode(),
                                "medicineName", shortage.medicineName(),
                                "requiredQuantity", shortage.requiredQuantity(),
                                "availableQuantity", shortage.availableQuantity(),
                                "shortageQuantity", shortage.shortageQuantity()
                        )).toList()
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Validation failed.",
                request.getRequestURI(),
                Map.of("fields", errors)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Invalid parameter: " + ex.getName(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        if (ex.getCause() instanceof JsonMappingException mappingException
                && !mappingException.getPath().isEmpty()) {
            String field = toFieldPath(mappingException);
            return build(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Validation failed.",
                    request.getRequestURI(),
                    Map.of("fields", Map.of(field, "Invalid value."))
            );
        }

        return build(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                "Malformed JSON request.",
                request.getRequestURI()
        );
    }

    private String toFieldPath(JsonMappingException exception) {
        StringBuilder field = new StringBuilder();
        for (JsonMappingException.Reference reference : exception.getPath()) {
            if (reference.getFieldName() != null) {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                field.append('[').append(reference.getIndex()).append(']');
            }
        }
        return field.toString();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                ex.getParameterName() + " is required.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "Access denied.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestPart(
            MissingServletRequestPartException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_REQUEST_PART", ex.getRequestPartName() + " is required.", request.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Uploaded file exceeds the allowed size.", request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found.", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error("Unhandled request failure for {}", request.getRequestURI(), ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Internal server error.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        return build(status, code, message, path, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, Object> details
    ) {

        return ResponseEntity
                .status(status)
                .body(ApiErrorResponseFactory.create(status, code, message, path, details));
    }

}

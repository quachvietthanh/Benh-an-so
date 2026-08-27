package com.benhsoan.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

final class DomainExceptionHttpStatusMapper {

    private DomainExceptionHttpStatusMapper() {
    }

    static HttpStatus statusFor(DomainErrorCode code) {
        return switch (code) {
            case APPOINTMENT_NOT_FOUND,
                    BACKUP_NOT_FOUND,
                    CARE_LOG_NOT_FOUND,
                    CLINICAL_ATTACHMENT_NOT_FOUND,
                    CLINICAL_ORDER_ITEM_NOT_FOUND,
                    CLINICAL_RESULT_NOT_FOUND,
                    DIAGNOSIS_CATALOG_NOT_FOUND,
                    DOCTOR_NOT_FOUND,
                    DRUG_INTERACTION_NOT_FOUND,
                    FOLLOW_UP_REMINDER_NOT_FOUND,
                    INVOICE_NOT_FOUND,
                    MEDICAL_RECORD_NOT_FOUND,
                    MEDICAL_RECORD_TEMPLATE_NOT_FOUND,
                    MEDICINE_NOT_FOUND,
                    PATIENT_NOT_FOUND,
                    PAYMENT_NOT_FOUND,
                    PORTAL_LOOKUP_NOT_FOUND,
                    PRESCRIPTION_ITEM_NOT_FOUND,
                    PRESCRIPTION_NOT_FOUND,
                    QUEUE_ITEM_NOT_FOUND,
                    QUEUE_NOT_FOUND,
                    ROLE_NOT_FOUND,
                    ROOM_NOT_FOUND,
                    SERVICE_CATALOG_NOT_FOUND,
                    SPECIALTY_NOT_FOUND,
                    USER_NOT_FOUND,
                    VISIT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ACCOUNT_DISABLED,
                    ACCOUNT_LOCKED,
                    INVOICE_UNAUTHORIZED_ADJUSTMENT,
                    MEDICAL_RECORD_ACCESS_DENIED,
                    MEDICAL_RECORD_UNAUTHORIZED_RECIPIENT,
                    MEDICAL_RECORD_UNAUTHORIZED_SIGNER,
                    PATIENT_INACTIVE,
                    UNAUTHORIZED_APPOINTMENT_OPERATION,
                    UNAUTHORIZED_PRESCRIPTION_AMENDMENT,
                    UNAUTHORIZED_QUEUE_OPERATION,
                    VISIT_ENCOUNTER_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case INVALID_CREDENTIALS,
                    SESSION_EXPIRED,
                    TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case APPOINTMENT_TIME_IN_PAST,
                    DOCTOR_SCHEDULE_UNAVAILABLE,
                    INVALID_BACKUP_STATUS,
                    INVALID_DOCTOR_ROLE,
                    MEDICAL_RECORD_MISSING_AUTHORIZATION,
                    MEDICAL_RECORD_MISSING_DIAGNOSIS,
                    MEDICAL_RECORD_NOT_LOCKED,
                    MEDICAL_RECORD_NOT_SIGNED,
                    PAYMENT_AMOUNT_MISMATCH,
                    SELF_DRUG_INTERACTION,
                    VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            case BACKUP_EXECUTION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            case REPORT_DATA_EMPTY -> HttpStatus.UNPROCESSABLE_ENTITY;
            case TOO_MANY_LOGIN_ATTEMPTS -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.CONFLICT;
        };
    }
}

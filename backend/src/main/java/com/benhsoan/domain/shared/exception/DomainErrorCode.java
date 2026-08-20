package com.benhsoan.domain.shared.exception;

import java.util.Arrays;

/**
 * Stable API error codes for every concrete domain exception.
 */
public enum DomainErrorCode {
    ACCOUNT_DISABLED("AccountDisabledException"),
    ACCOUNT_LOCKED("AccountLockedException"),
    APPOINTMENT_ALREADY_CANCELLED("AppointmentAlreadyCancelledException"),
    APPOINTMENT_ALREADY_COMPLETED("AppointmentAlreadyCompletedException"),
    APPOINTMENT_ALREADY_IN_PROGRESS("AppointmentAlreadyInProgressException"),
    APPOINTMENT_INVALID_STATUS("AppointmentInvalidStatusException"),
    APPOINTMENT_NOT_FOUND("AppointmentNotFoundException"),
    APPOINTMENT_NOT_OVERDUE("AppointmentNotOverdueException"),
    APPOINTMENT_TIME_CONFLICT("AppointmentTimeConflictException"),
    APPOINTMENT_TIME_IN_PAST("AppointmentTimeInPastException"),
    BACKUP_EXECUTION_FAILED("BackupExecutionException"),
    BACKUP_NOT_FOUND("BackupNotFoundException"),
    CARE_LOG_NOT_FOUND("CareLogNotFoundException"),
    CHECK_IN_CONFLICT("CheckInConflictException"),
    CLINICAL_ORDER_ALREADY_CANCELLED("ClinicalOrderAlreadyCancelledException"),
    CLINICAL_ORDER_ALREADY_COMPLETED("ClinicalOrderAlreadyCompletedException"),
    CLINICAL_ORDER_INVALID_STATUS("ClinicalOrderInvalidStatusException"),
    CLINICAL_ORDER_INVALID_VISIT("ClinicalOrderInvalidVisitException"),
    CLINICAL_ORDER_ITEM_INVALID_STATUS("ClinicalOrderItemInvalidStatusException"),
    CLINICAL_ORDER_LOCKED_MEDICAL_RECORD("ClinicalOrderLockedMedicalRecordException"),
    CLINICAL_RESULT_ALREADY_FINALIZED("ClinicalResultAlreadyFinalizedException"),
    CLINICAL_RESULT_INVALID_STATUS("ClinicalResultInvalidStatusException"),
    CLINICAL_SERVICE_UNAVAILABLE("ClinicalServiceUnavailableException"),
    DOCTOR_INACTIVE("DoctorInactiveException"),
    DOCTOR_NOT_FOUND("DoctorNotFoundException"),
    DOCTOR_ROOM_ASSIGNMENT_CONFLICT("DoctorRoomAssignmentConflictException"),
    DOCTOR_NOT_ASSIGNED_TO_ROOM("DoctorNotAssignedToRoomException"),
    DRUG_INTERACTION_ALREADY_EXISTS("DrugInteractionAlreadyExistsException"),
    DRUG_INTERACTION_NOT_FOUND("DrugInteractionNotFoundException"),
    EMAIL_ALREADY_EXISTS("EmailAlreadyExistsException"),
    FOLLOW_UP_REMINDER_INVALID_STATUS("FollowUpReminderInvalidStatusException"),
    FOLLOW_UP_REMINDER_NOT_FOUND("FollowUpReminderNotFoundException"),
    INSUFFICIENT_STOCK("PrescriptionInsufficientStockException", "PrescriptionAllocationInsufficientStockException"),
    INTERACTION_CONFIRMATION_REQUIRED("PrescriptionInteractionConfirmationRequiredException"),
    INVALID_BACKUP_STATUS("InvalidBackupStatusException"),
    INVALID_CREDENTIALS("InvalidCredentialsException"),
    INVOICE_ALREADY_ISSUED("InvoiceAlreadyIssuedException"),
    INVOICE_NOT_FOUND("InvoiceNotFoundException"),
    INVOICE_UNAUTHORIZED_ADJUSTMENT("InvoiceUnauthorizedAdjustmentException"),
    LAST_ADMINISTRATOR_PERMISSION("LastAdministratorPermissionException"),
    MEDICAL_RECORD_ACCESS_DENIED("MedicalRecordAccessDeniedException"),
    MEDICAL_RECORD_ALREADY_EXISTS_FOR_VISIT("MedicalRecordAlreadyExistsForVisitException"),
    MEDICAL_RECORD_AMENDMENT_REQUIRES_COMPLETED_VISIT("MedicalRecordAmendmentRequiresCompletedVisitException"),
    MEDICAL_RECORD_INVALID_STATUS("MedicalRecordInvalidStatusException"),
    MEDICAL_RECORD_INVALID_VISIT("MedicalRecordInvalidVisitException"),
    MEDICAL_RECORD_LOCKED("MedicalRecordAlreadyLockedException"),
    MEDICAL_RECORD_NOT_FOUND("MedicalRecordNotFoundException"),
    MEDICAL_RECORD_NOT_LOCKED("MedicalRecordNotLockedException"),
    MEDICINE_CODE_ALREADY_EXISTS("MedicineCodeAlreadyExistsException"),
    MEDICINE_INACTIVE("MedicineInactiveException"),
    MEDICINE_NOT_FOUND("MedicineNotFoundException"),
    PATIENT_ALREADY_EXISTS("PatientAlreadyExistsException"),
    PATIENT_INACTIVE("PatientInactiveException"),
    PATIENT_NOT_FOUND("PatientNotFoundException"),
    PAYMENT_ALREADY_EXISTS("PaymentAlreadyExistsException"),
    PAYMENT_AMOUNT_MISMATCH("PaymentAmountMismatchException"),
    PAYMENT_NOT_ALLOWED("PaymentNotAllowedException"),
    PAYMENT_NOT_FOUND("PaymentNotFoundException"),
    PAYMENT_REQUIRED_FOR_INVOICE("PaymentRequiredForInvoiceException"),
    PORTAL_LOOKUP_NOT_FOUND("PortalLookupNotFoundException"),
    PRESCRIPTION_ALREADY_CANCELLED("PrescriptionAlreadyCancelledException"),
    PRESCRIPTION_ALREADY_DISPENSED("PrescriptionAlreadyDispensedException"),
    PRESCRIPTION_CLINICAL_CONTEXT_CONFLICT("PrescriptionClinicalContextConflictException"),
    PRESCRIPTION_INVALID_STATUS("PrescriptionInvalidStatusException"),
    PRESCRIPTION_ITEM_NOT_FOUND("PrescriptionItemNotFoundException"),
    PRESCRIPTION_NO_CHANGES("PrescriptionNoChangesException"),
    PRESCRIPTION_NOT_FOUND("PrescriptionNotFoundException"),
    PRESCRIPTION_NOT_PRINTABLE("PrescriptionNotPrintableException"),
    QUEUE_ITEM_INVALID_STATUS("QueueItemInvalidStatusException"),
    QUEUE_ITEM_NOT_FOUND("QueueItemNotFoundException"),
    QUEUE_NOT_FOUND("QueueNotFoundException"),
    REPORT_DATA_EMPTY("OperationalReportDataEmptyException"),
    ROLE_NOT_FOUND("RoleNotFoundException"),
    ROOM_CODE_ALREADY_EXISTS("RoomCodeAlreadyExistsException"),
    ROOM_NOT_FOUND("RoomNotFoundException"),
    SELF_DRUG_INTERACTION("SelfDrugInteractionException"),
    SESSION_EXPIRED("SessionExpiredException"),
    TOKEN_INVALID("TokenInvalidException"),
    TOO_MANY_LOGIN_ATTEMPTS("TooManyLoginAttemptsException"),
    UNAUTHORIZED_APPOINTMENT_OPERATION("UnauthorizedAppointmentOperationException"),
    UNAUTHORIZED_PRESCRIPTION_AMENDMENT("UnauthorizedPrescriptionAmendmentException"),
    UNAUTHORIZED_QUEUE_OPERATION("UnauthorizedQueueOperationException"),
    USER_ALREADY_EXISTS("UserAlreadyExistsException"),
    USER_NOT_FOUND("UserNotFoundException"),
    VALIDATION_FAILED("ValidationException"),
    VISIT_ALREADY_CANCELLED("VisitAlreadyCancelledException"),
    VISIT_ALREADY_COMPLETED("VisitAlreadyCompletedException"),
    VISIT_ENCOUNTER_ACCESS_DENIED("VisitEncounterAccessDeniedException"),
    VISIT_INVALID_STATUS("VisitInvalidStatusException"),
    VISIT_NOT_FOUND("VisitNotFoundException");

    private final String[] exceptionSimpleNames;

    DomainErrorCode(String... exceptionSimpleNames) {
        this.exceptionSimpleNames = exceptionSimpleNames;
    }

    public static DomainErrorCode forException(Class<? extends DomainException> exceptionType) {
        String simpleName = exceptionType.getSimpleName();
        return Arrays.stream(values())
                .filter(code -> Arrays.asList(code.exceptionSimpleNames).contains(simpleName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No stable error code registered for " + exceptionType.getName()
                ));
    }
}

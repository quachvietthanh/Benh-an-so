package com.benhsoan.domain.clinic;

import java.time.Instant;
import java.time.LocalTime;

import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.Getter;

@Getter
public class ClinicConfiguration {

    public static final int SINGLETON_ID = 1;

    public static final int DEFAULT_RETENTION_YEARS = 10;
    public static final int MIN_RETENTION_YEARS = 10;

    public static final int DEFAULT_SIGNING_DEADLINE_HOURS = 24;
    public static final int MIN_SIGNING_DEADLINE_HOURS = 1;

    public static final int DEFAULT_ACTIVE_RECORD_DURATION_MONTHS = 12;
    public static final int MIN_ACTIVE_RECORD_DURATION_MONTHS = 1;

    public static final int DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES = 30;
    public static final int MIN_SESSION_IDLE_TIMEOUT_MINUTES = 5;
    public static final int MAX_SESSION_IDLE_TIMEOUT_MINUTES = 1440;

    private static final int MAX_CLINIC_NAME_LENGTH = 150;
    private static final int MAX_ADDRESS_LENGTH = 500;
    private static final int MAX_PHONE_LENGTH = 30;

    private final int id;
    private String clinicName;
    private String address;
    private String phone;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private int retentionYears;
    private int signingDeadlineHours;
    private int activeRecordDurationMonths;
    private int sessionIdleTimeoutMinutes;
    private final Instant createdAt;
    private Instant updatedAt;

    private ClinicConfiguration(
            int id,
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            int signingDeadlineHours,
            int activeRecordDurationMonths,
            int sessionIdleTimeoutMinutes,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (id != SINGLETON_ID) {
            throw new ValidationException("Clinic configuration id must be 1.");
        }
        this.id = id;
        this.clinicName = normalizeRequired(clinicName, "Clinic name", MAX_CLINIC_NAME_LENGTH);
        this.address = normalizeOptional(address, "Address", MAX_ADDRESS_LENGTH);
        this.phone = normalizeOptional(phone, "Phone", MAX_PHONE_LENGTH);
        this.openingTime = Guard.require(openingTime, "Opening time");
        this.closingTime = Guard.require(closingTime, "Closing time");
        validateWorkingHours(this.openingTime, this.closingTime);
        this.retentionYears = validateRetentionYears(retentionYears);
        this.signingDeadlineHours = validateSigningDeadlineHours(signingDeadlineHours);
        this.activeRecordDurationMonths = validateActiveRecordDurationMonths(activeRecordDurationMonths);
        this.sessionIdleTimeoutMinutes = validateSessionIdleTimeoutMinutes(sessionIdleTimeoutMinutes);
        this.createdAt = Guard.require(createdAt, "Created at");
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public static ClinicConfiguration create(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            Instant now
    ) {
        return create(clinicName, address, phone, openingTime, closingTime, DEFAULT_RETENTION_YEARS, DEFAULT_SIGNING_DEADLINE_HOURS, DEFAULT_ACTIVE_RECORD_DURATION_MONTHS, DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES, now);
    }

    public static ClinicConfiguration create(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            Instant now
    ) {
        return create(clinicName, address, phone, openingTime, closingTime, retentionYears, DEFAULT_SIGNING_DEADLINE_HOURS, DEFAULT_ACTIVE_RECORD_DURATION_MONTHS, DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES, now);
    }

    public static ClinicConfiguration create(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            int signingDeadlineHours,
            Instant now
    ) {
        return create(clinicName, address, phone, openingTime, closingTime, retentionYears, signingDeadlineHours, DEFAULT_ACTIVE_RECORD_DURATION_MONTHS, DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES, now);
    }

    public static ClinicConfiguration create(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            int signingDeadlineHours,
            int sessionIdleTimeoutMinutes,
            Instant now
    ) {
        return create(clinicName, address, phone, openingTime, closingTime, retentionYears, signingDeadlineHours, DEFAULT_ACTIVE_RECORD_DURATION_MONTHS, sessionIdleTimeoutMinutes, now);
    }

    public static ClinicConfiguration create(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            int signingDeadlineHours,
            int activeRecordDurationMonths,
            int sessionIdleTimeoutMinutes,
            Instant now
    ) {
        return new ClinicConfiguration(
                SINGLETON_ID, clinicName, address, phone, openingTime, closingTime, retentionYears, signingDeadlineHours, activeRecordDurationMonths, sessionIdleTimeoutMinutes, now, now
        );
    }

    public static ClinicConfiguration restore(
            int id,
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            Instant createdAt,
            Instant updatedAt
    ) {
        return restore(id, clinicName, address, phone, openingTime, closingTime, DEFAULT_RETENTION_YEARS, DEFAULT_SIGNING_DEADLINE_HOURS, DEFAULT_ACTIVE_RECORD_DURATION_MONTHS, DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES, createdAt, updatedAt);
    }

    public static ClinicConfiguration restore(
            int id,
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            Instant createdAt,
            Instant updatedAt
    ) {
        return restore(id, clinicName, address, phone, openingTime, closingTime, retentionYears, DEFAULT_SIGNING_DEADLINE_HOURS, DEFAULT_ACTIVE_RECORD_DURATION_MONTHS, DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES, createdAt, updatedAt);
    }

    public static ClinicConfiguration restore(
            int id,
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            int signingDeadlineHours,
            Instant createdAt,
            Instant updatedAt
    ) {
        return restore(id, clinicName, address, phone, openingTime, closingTime, retentionYears, signingDeadlineHours, DEFAULT_ACTIVE_RECORD_DURATION_MONTHS, DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES, createdAt, updatedAt);
    }

    public static ClinicConfiguration restore(
            int id,
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            int signingDeadlineHours,
            int activeRecordDurationMonths,
            int sessionIdleTimeoutMinutes,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ClinicConfiguration(
                id, clinicName, address, phone, openingTime, closingTime, retentionYears, signingDeadlineHours, activeRecordDurationMonths, sessionIdleTimeoutMinutes, createdAt, updatedAt
        );
    }

    public void update(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            Instant updatedAt
    ) {
        this.clinicName = normalizeRequired(clinicName, "Clinic name", MAX_CLINIC_NAME_LENGTH);
        this.address = normalizeOptional(address, "Address", MAX_ADDRESS_LENGTH);
        this.phone = normalizeOptional(phone, "Phone", MAX_PHONE_LENGTH);
        this.openingTime = Guard.require(openingTime, "Opening time");
        this.closingTime = Guard.require(closingTime, "Closing time");
        validateWorkingHours(this.openingTime, this.closingTime);
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public void update(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            int signingDeadlineHours,
            Instant updatedAt
    ) {
        update(clinicName, address, phone, openingTime, closingTime, updatedAt);
        this.retentionYears = validateRetentionYears(retentionYears);
        this.signingDeadlineHours = validateSigningDeadlineHours(signingDeadlineHours);
    }

    public void update(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            int retentionYears,
            int signingDeadlineHours,
            int activeRecordDurationMonths,
            int sessionIdleTimeoutMinutes,
            Instant updatedAt
    ) {
        update(clinicName, address, phone, openingTime, closingTime, retentionYears, signingDeadlineHours, updatedAt);
        this.activeRecordDurationMonths = validateActiveRecordDurationMonths(activeRecordDurationMonths);
        this.sessionIdleTimeoutMinutes = validateSessionIdleTimeoutMinutes(sessionIdleTimeoutMinutes);
    }

    public void updateRetentionYears(int retentionYears, Instant updatedAt) {
        this.retentionYears = validateRetentionYears(retentionYears);
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public void updateSigningDeadlineHours(int signingDeadlineHours, Instant updatedAt) {
        this.signingDeadlineHours = validateSigningDeadlineHours(signingDeadlineHours);
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public void updateActiveRecordDurationMonths(int activeRecordDurationMonths, Instant updatedAt) {
        this.activeRecordDurationMonths = validateActiveRecordDurationMonths(activeRecordDurationMonths);
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public void updateSessionIdleTimeoutMinutes(int sessionIdleTimeoutMinutes, Instant updatedAt) {
        this.sessionIdleTimeoutMinutes = validateSessionIdleTimeoutMinutes(sessionIdleTimeoutMinutes);
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    private static String normalizeRequired(String value, String field, int maxLength) {
        String normalized = Guard.require(value, field).trim().replaceAll("\\s+", " ");
        if (normalized.length() > maxLength) {
            throw new ValidationException(field + " must not exceed " + maxLength + " characters.");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > maxLength) {
            throw new ValidationException(field + " must not exceed " + maxLength + " characters.");
        }
        return normalized;
    }

    private static void validateWorkingHours(LocalTime openingTime, LocalTime closingTime) {
        if (!closingTime.isAfter(openingTime)) {
            throw new ValidationException("Closing time must be after opening time.");
        }
    }

    private static int validateRetentionYears(int retentionYears) {
        if (retentionYears < MIN_RETENTION_YEARS) {
            throw new ValidationException("Retention years must be at least " + MIN_RETENTION_YEARS + ".");
        }
        return retentionYears;
    }

    private static int validateSigningDeadlineHours(int signingDeadlineHours) {
        if (signingDeadlineHours < MIN_SIGNING_DEADLINE_HOURS) {
            throw new ValidationException("Signing deadline hours must be at least " + MIN_SIGNING_DEADLINE_HOURS + ".");
        }
        return signingDeadlineHours;
    }

    private static int validateActiveRecordDurationMonths(int activeRecordDurationMonths) {
        if (activeRecordDurationMonths < MIN_ACTIVE_RECORD_DURATION_MONTHS) {
            throw new ValidationException("Active record duration months must be at least " + MIN_ACTIVE_RECORD_DURATION_MONTHS + ".");
        }
        return activeRecordDurationMonths;
    }

    private static int validateSessionIdleTimeoutMinutes(int sessionIdleTimeoutMinutes) {
        if (sessionIdleTimeoutMinutes < MIN_SESSION_IDLE_TIMEOUT_MINUTES || sessionIdleTimeoutMinutes > MAX_SESSION_IDLE_TIMEOUT_MINUTES) {
            throw new ValidationException("Session idle timeout must be between " + MIN_SESSION_IDLE_TIMEOUT_MINUTES + " and " + MAX_SESSION_IDLE_TIMEOUT_MINUTES + " minutes.");
        }
        return sessionIdleTimeoutMinutes;
    }
}

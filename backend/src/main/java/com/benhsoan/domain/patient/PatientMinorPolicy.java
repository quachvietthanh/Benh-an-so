package com.benhsoan.domain.patient;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

/**
 * Domain policy defining rules and age calculations for minors and guardians
 * according to NCL-02-CN-008 and QTN-44.
 *
 * Age threshold is 18 (Law on Medical Examination and Treatment & Civil Code 2015).
 */
public final class PatientMinorPolicy {

    public static final int MINOR_AGE_THRESHOLD = 18;
    public static final ZoneId CLINICAL_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private PatientMinorPolicy() {
    }

    public static LocalDate currentDate() {
        return LocalDate.now(CLINICAL_TIMEZONE);
    }

    public static int calculateAge(LocalDate dateOfBirth, LocalDate asOfDate) {
        if (dateOfBirth == null) {
            return 0;
        }
        LocalDate reference = asOfDate != null ? asOfDate : currentDate();
        if (dateOfBirth.isAfter(reference)) {
            return 0;
        }
        return Period.between(dateOfBirth, reference).getYears();
    }

    public static int calculateAge(LocalDate dateOfBirth) {
        return calculateAge(dateOfBirth, currentDate());
    }

    public static boolean isMinor(LocalDate dateOfBirth, LocalDate asOfDate) {
        return calculateAge(dateOfBirth, asOfDate) < MINOR_AGE_THRESHOLD;
    }

    public static boolean isMinor(LocalDate dateOfBirth) {
        return isMinor(dateOfBirth, currentDate());
    }

    public static boolean requiresAdultTransition(LocalDate dateOfBirth, String guardianName, LocalDate asOfDate) {
        return !isMinor(dateOfBirth, asOfDate) && guardianName != null && !guardianName.isBlank();
    }

    public static boolean requiresAdultTransition(LocalDate dateOfBirth, String guardianName) {
        return requiresAdultTransition(dateOfBirth, guardianName, currentDate());
    }
}

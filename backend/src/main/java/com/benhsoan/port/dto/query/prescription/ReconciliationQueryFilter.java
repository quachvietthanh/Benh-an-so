package com.benhsoan.port.dto.query.prescription;

import java.time.Instant;
import java.util.List;

import com.benhsoan.domain.shared.exception.ValidationException;

/**
 * Technical filter handed to the reconciliation query repository.
 *
 * Period semantics (locked): a prescription belongs to [from, to) when its
 * prescribedAt, its lastInterconnectionAt, or any dispense item dispensedAt falls inside
 * the half open interval. Nulls mean unbounded on that side.
 *
 * outcomeGroups use OR semantics and hold at most two blocks because the API can express
 * either one explicit outcome or the two discrepancy outcomes.
 */
public record ReconciliationQueryFilter(
        Instant fromInclusive,
        Instant toExclusive,
        String prescriptionCode,
        List<ReconciliationOutcomeGroup> outcomeGroups
) {

    public static final int MAX_OUTCOME_GROUPS = 2;

    public ReconciliationQueryFilter {
        outcomeGroups = List.copyOf(outcomeGroups);
        if (outcomeGroups.size() > MAX_OUTCOME_GROUPS) {
            throw new ValidationException("At most two reconciliation outcome groups are supported.");
        }
    }

    /** True when neither bound is supplied, so the period predicate must be skipped. */
    public boolean periodUnbounded() {
        return fromInclusive == null && toExclusive == null;
    }
}
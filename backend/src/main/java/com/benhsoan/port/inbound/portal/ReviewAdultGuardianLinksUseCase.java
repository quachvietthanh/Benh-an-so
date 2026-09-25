package com.benhsoan.port.inbound.portal;

/**
 * NCL-14-CN-010 TC-03: reviews guardian links whose dependent has reached adulthood and
 * notifies the affected accounts that the link should be reviewed.
 *
 * The sweep never removes the guardian link; removal stays with the existing authorized
 * staff flow. Repeated invocations are idempotent.
 */
public interface ReviewAdultGuardianLinksUseCase {

    /**
     * @return the number of dependent profiles whose guardian link was reviewed and found to
     *         require removal, i.e. the number of adult dependents that still hold a guardian
     *         link (not the number of notifications written, which is guarded for idempotency).
     */
    int reviewAdultGuardianLinks();
}

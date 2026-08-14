package com.benhsoan.port.outbound.repository.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OperationalReportQueryRepository {

    long countCompletedVisits(Instant fromInclusive, Instant toExclusive);

    List<DailyVisitSummary> findDailyCompletedVisits(Instant fromInclusive, Instant toExclusive);

    BigDecimal sumNetRevenue(Instant fromInclusive, Instant toExclusive);

    List<DailyRevenueSummary> findDailyNetRevenue(Instant fromInclusive, Instant toExclusive);

    List<TopMedicineSummary> findTopDispensedMedicines(Instant fromInclusive, Instant toExclusive);

    List<DoctorVisitSummary> findDoctorVisitSummaries(Instant fromInclusive, Instant toExclusive);
}

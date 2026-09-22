package com.benhsoan.persistence.adapterRepository.reporting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.port.outbound.repository.reporting.AppointmentEffectivenessQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.AppointmentStatusCountSummary;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentEffectivenessQueryRepositoryAdapter implements AppointmentEffectivenessQueryRepository {

    private static final String PORTAL_CHANNEL = "ONLINE_PORTAL";
    private static final String COUNTER_CHANNEL = "RECEPTION_COUNTER";

    private final EntityManager entityManager;

    @Override
    public List<AppointmentStatusCountSummary> findStatusCounts(
            Instant fromInclusive,
            Instant toExclusive,
            UUID doctorId,
            String bookingChannel
    ) {
        if (bookingChannel != null
                && !PORTAL_CHANNEL.equals(bookingChannel)
                && !COUNTER_CHANNEL.equals(bookingChannel)) {
            throw new ValidationException(
                    "bookingChannel must be one of: ONLINE_PORTAL, RECEPTION_COUNTER.");
        }

        String jpql = """
                select coalesce(a.bookingChannel, :counterChannel), a.status, count(a.id)
                from AppointmentEntity a
                where a.startTime >= :fromInclusive
                  and a.startTime < :toExclusive
                """
                + (doctorId != null ? "  and a.doctorId = :doctorId\n" : "\n")
                + (PORTAL_CHANNEL.equals(bookingChannel)
                        ? "  and a.bookingChannel = :bookingChannel\n"
                        : COUNTER_CHANNEL.equals(bookingChannel)
                                ? "  and a.bookingChannel is null\n"
                                : "\n")
                + "group by coalesce(a.bookingChannel, :counterChannel), a.status\n";

        var query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .setParameter("counterChannel", COUNTER_CHANNEL);

        if (doctorId != null) {
            query.setParameter("doctorId", doctorId);
        }
        if (PORTAL_CHANNEL.equals(bookingChannel)) {
            query.setParameter("bookingChannel", bookingChannel);
        }

        return query.getResultList().stream()
                .map(row -> new AppointmentStatusCountSummary(
                        (String) row[0],
                        (AppointmentStatus) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
    }
}

package com.benhsoan.persistence.adapterRepository.reporting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.port.outbound.repository.reporting.AppointmentEffectivenessQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.AppointmentStatusCountSummary;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentEffectivenessQueryRepositoryAdapter implements AppointmentEffectivenessQueryRepository {

    private final EntityManager entityManager;

    @Override
    public List<AppointmentStatusCountSummary> findStatusCounts(
            Instant fromInclusive,
            Instant toExclusive,
            UUID doctorId,
            String bookingChannel
    ) {
        String jpql = """
                select a.status, count(a.id)
                from AppointmentEntity a
                where a.startTime >= :fromInclusive
                  and a.startTime < :toExclusive
                """
                + (doctorId != null ? "  and a.doctorId = :doctorId\n" : "\n")
                + ("ONLINE_PORTAL".equals(bookingChannel)
                        ? "  and a.bookingChannel = :bookingChannel\n"
                        : "RECEPTION_COUNTER".equals(bookingChannel)
                                ? "  and a.bookingChannel is null\n"
                                : "\n")
                + "group by a.status\n";

        var query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive);

        if (doctorId != null) {
            query.setParameter("doctorId", doctorId);
        }
        if ("ONLINE_PORTAL".equals(bookingChannel)) {
            query.setParameter("bookingChannel", bookingChannel);
        }

        return query.getResultList().stream()
                .map(row -> new AppointmentStatusCountSummary(
                        (AppointmentStatus) row[0],
                        ((Number) row[1]).longValue()))
                .toList();
    }
}

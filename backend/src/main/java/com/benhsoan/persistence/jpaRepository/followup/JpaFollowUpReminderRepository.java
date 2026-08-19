package com.benhsoan.persistence.jpaRepository.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.persistence.entity.followup.FollowUpReminderEntity;

public interface JpaFollowUpReminderRepository
        extends JpaRepository<FollowUpReminderEntity, UUID> {

    @Query("""
            select reminder
            from FollowUpReminderEntity reminder
            where (:patientId is null or reminder.patientId = :patientId)
              and (:status is null or reminder.status = :status)
              and (:fromDate is null or reminder.followUpDate >= :fromDate)
              and (:toDate is null or reminder.followUpDate <= :toDate)
            order by reminder.remindAt asc
            """)
    Page<FollowUpReminderEntity> search(
            @Param("patientId") UUID patientId,
            @Param("status") ReminderStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("""
            select reminder
            from FollowUpReminderEntity reminder
            where reminder.status = com.benhsoan.domain.followup.enums.ReminderStatus.PENDING
              and reminder.remindAt <= :currentInstant
              and (:fromDate is null or reminder.followUpDate >= :fromDate)
              and (:toDate is null or reminder.followUpDate <= :toDate)
            order by reminder.remindAt asc
            """)
    Page<FollowUpReminderEntity> findDue(
            @Param("currentInstant") Instant currentInstant,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );
}

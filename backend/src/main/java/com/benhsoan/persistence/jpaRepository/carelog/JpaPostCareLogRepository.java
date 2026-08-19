package com.benhsoan.persistence.jpaRepository.carelog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.persistence.entity.carelog.PostCareLogEntity;

public interface JpaPostCareLogRepository
        extends JpaRepository<PostCareLogEntity, UUID> {

    List<PostCareLogEntity> findByPatientIdOrderByContactedAtDesc(UUID patientId);

    @Query("""
            select log
            from PostCareLogEntity log
            where (:channel is null or log.contactChannel = :channel)
              and (:from is null or log.contactedAt >= :from)
              and (:to is null or log.contactedAt < :to)
            order by log.contactedAt desc
            """)
    Page<PostCareLogEntity> search(
            @Param("channel") ContactChannel channel,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}

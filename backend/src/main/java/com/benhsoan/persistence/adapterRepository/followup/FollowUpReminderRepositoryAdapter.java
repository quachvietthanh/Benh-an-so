package com.benhsoan.persistence.adapterRepository.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.persistence.jpaRepository.followup.JpaFollowUpReminderRepository;
import com.benhsoan.persistence.mapper.followup.FollowUpReminderPersistenceMapper;
import com.benhsoan.port.dto.command.followup.SearchFollowUpRemindersQuery;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FollowUpReminderRepositoryAdapter implements FollowUpReminderRepository {

    private final JpaFollowUpReminderRepository jpaRepository;
    private final FollowUpReminderPersistenceMapper mapper;

    @Override
    @Transactional
    public FollowUpReminder save(FollowUpReminder reminder) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(reminder)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FollowUpReminder> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUpReminder> search(SearchFollowUpRemindersQuery query) {
        return jpaRepository.search(
                        query.patientId(),
                        query.status(),
                        query.fromDate(),
                        query.toDate(),
                        query.pageable()
                )
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUpReminder> findDue(Instant currentInstant, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return jpaRepository.findDue(currentInstant, fromDate, toDate, pageable)
                .map(mapper::toDomain);
    }
}

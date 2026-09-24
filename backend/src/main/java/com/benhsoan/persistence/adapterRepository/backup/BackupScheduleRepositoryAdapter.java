package com.benhsoan.persistence.adapterRepository.backup;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.backup.BackupSchedule;
import com.benhsoan.persistence.jpaRepository.backup.JpaBackupScheduleRepository;
import com.benhsoan.persistence.mapper.backup.BackupSchedulePersistenceMapper;
import com.benhsoan.port.outbound.repository.backup.BackupScheduleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BackupScheduleRepositoryAdapter implements BackupScheduleRepository {

    private final JpaBackupScheduleRepository jpaRepository;
    private final BackupSchedulePersistenceMapper mapper;

    @Override
    public Optional<BackupSchedule> find() {
        return jpaRepository.findById(BackupSchedule.SINGLETON_ID).map(mapper::toDomain);
    }

    @Override
    public BackupSchedule save(BackupSchedule schedule) {
        Objects.requireNonNull(schedule, "Backup schedule must not be null.");
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(schedule)));
    }
}

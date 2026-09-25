package com.benhsoan.persistence.adapterRepository.backup;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.backup.BackupScheduleConfiguration;
import com.benhsoan.persistence.jpaRepository.backup.JpaBackupScheduleConfigurationRepository;
import com.benhsoan.persistence.mapper.backup.BackupSchedulePersistenceMapper;
import com.benhsoan.port.outbound.repository.backup.BackupScheduleRepositoryPort;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BackupScheduleRepositoryAdapter implements BackupScheduleRepositoryPort {

    private final JpaBackupScheduleConfigurationRepository jpaRepository;
    private final BackupSchedulePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<BackupScheduleConfiguration> find() {
        return jpaRepository.findFirstByOrderByUpdatedAtDesc().map(mapper::toDomain);
    }

    @Override
    @Transactional
    public BackupScheduleConfiguration save(BackupScheduleConfiguration configuration) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(configuration)));
    }
}

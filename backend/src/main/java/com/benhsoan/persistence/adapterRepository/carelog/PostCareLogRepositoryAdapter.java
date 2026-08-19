package com.benhsoan.persistence.adapterRepository.carelog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.carelog.PostCareLog;
import com.benhsoan.persistence.jpaRepository.carelog.JpaPostCareLogRepository;
import com.benhsoan.persistence.mapper.carelog.PostCareLogPersistenceMapper;
import com.benhsoan.port.dto.command.carelog.SearchPostCareLogsQuery;
import com.benhsoan.port.outbound.repository.carelog.PostCareLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostCareLogRepositoryAdapter implements PostCareLogRepository {

    private final JpaPostCareLogRepository jpaRepository;
    private final PostCareLogPersistenceMapper mapper;

    @Override
    @Transactional
    public PostCareLog save(PostCareLog careLog) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(careLog)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostCareLog> findByPatientIdOrderByContactedAtDesc(UUID patientId) {
        return jpaRepository.findByPatientIdOrderByContactedAtDesc(patientId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostCareLog> search(SearchPostCareLogsQuery query) {
        return jpaRepository.search(query.channel(), query.from(), query.to(), query.pageable())
                .map(mapper::toDomain);
    }
}

package com.benhsoan.persistence.adapterRepository.appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.persistence.entity.appointment.DoctorTimeOffEntity;
import com.benhsoan.persistence.jpaRepository.appointment.JpaDoctorTimeOffRepository;
import com.benhsoan.persistence.mapper.appointment.DoctorTimeOffPersistenceMapper;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DoctorTimeOffRepositoryAdapter implements DoctorTimeOffRepository {

    private final JpaDoctorTimeOffRepository jpaRepository;
    private final DoctorTimeOffPersistenceMapper mapper;

    @Override
    public Optional<DoctorTimeOff> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public DoctorTimeOff save(DoctorTimeOff timeOff) {
        DoctorTimeOffEntity entity = mapper.toEntity(timeOff);
        DoctorTimeOffEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<DoctorTimeOff> findOverlappingActiveTimeOffs(UUID doctorId, Instant startTime, Instant endTime) {
        return jpaRepository.findTimeOffsOverlapping(doctorId, TimeOffStatus.ACTIVE, startTime, endTime).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<DoctorTimeOff> search(UUID doctorId, TimeOffStatus status, Instant fromTime, Instant toTime) {
        return jpaRepository.search(doctorId, status, fromTime, toTime).stream()
                .map(mapper::toDomain)
                .toList();
    }

}

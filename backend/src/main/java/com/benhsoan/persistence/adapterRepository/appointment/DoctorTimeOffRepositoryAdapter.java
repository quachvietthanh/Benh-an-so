package com.benhsoan.persistence.adapterRepository.appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.persistence.jpaRepository.appointment.JpaDoctorTimeOffRepository;
import com.benhsoan.persistence.mapper.appointment.DoctorTimeOffPersistenceMapper;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DoctorTimeOffRepositoryAdapter implements DoctorTimeOffRepository {

    private final JpaDoctorTimeOffRepository jpaRepository;
    private final DoctorTimeOffPersistenceMapper mapper;

    @Override
    public DoctorTimeOff save(DoctorTimeOff timeOff) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(timeOff)));
    }

    @Override
    public Optional<DoctorTimeOff> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DoctorTimeOff> findByDoctorId(UUID doctorId) {
        return jpaRepository.findByDoctorIdOrderByStartTimeDesc(doctorId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<DoctorTimeOff> findActiveOverlapping(UUID doctorId, Instant startTime, Instant endTime) {
        return jpaRepository.findOverlappingTimeOffs(doctorId, startTime, endTime, TimeOffStatus.ACTIVE).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveOverlapping(UUID doctorId, Instant startTime, Instant endTime) {
        return jpaRepository.existsOverlapping(doctorId, startTime, endTime, TimeOffStatus.ACTIVE);
    }
}

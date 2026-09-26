package com.benhsoan.persistence.adapterRepository.specialty;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.persistence.entity.specialty.DoctorSpecialtyEntity;
import com.benhsoan.persistence.jpaRepository.specialty.JpaDoctorSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.DoctorSpecialtyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DoctorSpecialtyRepositoryAdapter implements DoctorSpecialtyRepository {

    private final JpaDoctorSpecialtyRepository jpaRepository;

    @Override
    public void assignDoctors(UUID specialtyId, Collection<UUID> doctorIds, UUID assignedBy, Instant assignedAt) {
        if (doctorIds == null || doctorIds.isEmpty()) {
            return;
        }
        List<DoctorSpecialtyEntity> entities = doctorIds.stream()
                .distinct()
                .map(doctorId -> DoctorSpecialtyEntity.builder()
                        .id(UUID.randomUUID())
                        .doctorId(doctorId)
                        .specialtyId(specialtyId)
                        .assignedBy(assignedBy)
                        .assignedAt(assignedAt)
                        .build())
                .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public void removeAssignmentsBySpecialtyId(UUID specialtyId) {
        jpaRepository.deleteBySpecialtyId(specialtyId);
    }

    @Override
    public List<UUID> findDoctorIdsBySpecialtyId(UUID specialtyId) {
        return jpaRepository.findDoctorIdsBySpecialtyId(specialtyId);
    }

    @Override
    public List<UUID> findSpecialtyIdsByDoctorId(UUID doctorId) {
        return jpaRepository.findSpecialtyIdsByDoctorId(doctorId);
    }

    @Override
    public long countBySpecialtyId(UUID specialtyId) {
        return jpaRepository.countBySpecialtyId(specialtyId);
    }
}

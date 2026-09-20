package com.benhsoan.persistence.adapterRepository.specialty;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.persistence.entity.specialty.RoomSpecialtyEntity;
import com.benhsoan.persistence.jpaRepository.specialty.JpaRoomSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.RoomSpecialtyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoomSpecialtyRepositoryAdapter implements RoomSpecialtyRepository {

    private final JpaRoomSpecialtyRepository jpaRepository;

    @Override
    public void assignRooms(UUID specialtyId, Collection<UUID> roomIds, Instant assignedAt) {
        if (roomIds == null || roomIds.isEmpty()) {
            return;
        }
        List<RoomSpecialtyEntity> entities = roomIds.stream()
                .distinct()
                .map(roomId -> RoomSpecialtyEntity.builder()
                        .id(UUID.randomUUID())
                        .roomId(roomId)
                        .specialtyId(specialtyId)
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
    public List<UUID> findRoomIdsBySpecialtyId(UUID specialtyId) {
        return jpaRepository.findRoomIdsBySpecialtyId(specialtyId);
    }

    @Override
    public List<UUID> findSpecialtyIdsByRoomId(UUID roomId) {
        return jpaRepository.findSpecialtyIdsByRoomId(roomId);
    }

    @Override
    public long countBySpecialtyId(UUID specialtyId) {
        return jpaRepository.countBySpecialtyId(specialtyId);
    }
}

package com.benhsoan.persistence.adapterRepository.queue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.queue.Room;
import com.benhsoan.persistence.jpaRepository.queue.JpaRoomRepository;
import com.benhsoan.persistence.mapper.queue.QueueStructurePersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.queue.RoomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoomRepositoryAdapter implements RoomRepository {
    private final JpaRoomRepository jpaRepository;
    private final QueueStructurePersistenceMapper mapper;
    public Optional<Room> findActiveById(UUID roomId) { return jpaRepository.findByIdAndActiveTrue(roomId).map(mapper::toDomain); }
}

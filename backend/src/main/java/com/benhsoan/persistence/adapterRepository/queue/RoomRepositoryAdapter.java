package com.benhsoan.persistence.adapterRepository.queue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.queue.exception.RoomCodeAlreadyExistsException;
import com.benhsoan.persistence.jpaRepository.queue.JpaRoomRepository;
import com.benhsoan.persistence.mapper.queue.QueueStructurePersistenceMapper;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoomRepositoryAdapter implements RoomRepository {

    private final JpaRoomRepository jpaRepository;
    private final QueueStructurePersistenceMapper mapper;

    @Override
    public Optional<Room> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Room> findActiveById(UUID roomId) {
        return jpaRepository.findByIdAndActiveTrue(roomId).map(mapper::toDomain);
    }

    @Override
    public Room save(Room room) {
        try {
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(room)));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomCodeAlreadyExistsException(room.getCode());
        }
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCodeIgnoreCase(code);
    }

    @Override
    public Page<Room> search(String keyword, Boolean active, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return jpaRepository.search(normalizedKeyword, active, pageable).map(mapper::toDomain);
    }
}

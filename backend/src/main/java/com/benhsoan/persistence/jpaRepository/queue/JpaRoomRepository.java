package com.benhsoan.persistence.jpaRepository.queue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.queue.RoomEntity;

public interface JpaRoomRepository extends JpaRepository<RoomEntity, UUID> { Optional<RoomEntity> findByIdAndActiveTrue(UUID id); }

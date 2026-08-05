package com.benhsoan.port.outbound.repository.queue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.queue.Room;

public interface RoomRepository {

    Optional<Room> findById(UUID roomId);

    Room save(Room room);

    Optional<Room> findActiveById(UUID roomId);

    boolean existsByCode(String code);

    Page<Room> search(String keyword, Boolean active, Pageable pageable);
}

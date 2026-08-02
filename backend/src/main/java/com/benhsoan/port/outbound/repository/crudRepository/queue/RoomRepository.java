package com.benhsoan.port.outbound.repository.crudRepository.queue;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.queue.Room;

public interface RoomRepository { Optional<Room> findActiveById(UUID roomId); }

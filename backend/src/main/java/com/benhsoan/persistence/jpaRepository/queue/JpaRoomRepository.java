package com.benhsoan.persistence.jpaRepository.queue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.queue.RoomEntity;

public interface JpaRoomRepository extends JpaRepository<RoomEntity, UUID> {

    Optional<RoomEntity> findByIdAndActiveTrue(UUID id);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
            select room from RoomEntity room
            where (:active is null or room.active = :active)
              and (:keyword = ''
                   or lower(room.code) like lower(concat('%', :keyword, '%'))
                   or lower(room.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<RoomEntity> search(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable
    );
}

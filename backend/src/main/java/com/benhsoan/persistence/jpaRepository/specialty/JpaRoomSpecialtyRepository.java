package com.benhsoan.persistence.jpaRepository.specialty;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.specialty.RoomSpecialtyEntity;

public interface JpaRoomSpecialtyRepository extends JpaRepository<RoomSpecialtyEntity, UUID> {

    List<RoomSpecialtyEntity> findBySpecialtyId(UUID specialtyId);

    List<RoomSpecialtyEntity> findByRoomId(UUID roomId);

    long countBySpecialtyId(UUID specialtyId);

    void deleteBySpecialtyId(UUID specialtyId);

    @Query("SELECT rs.roomId FROM RoomSpecialtyEntity rs WHERE rs.specialtyId = :specialtyId")
    List<UUID> findRoomIdsBySpecialtyId(@Param("specialtyId") UUID specialtyId);

    @Query("SELECT rs.specialtyId FROM RoomSpecialtyEntity rs WHERE rs.roomId = :roomId")
    List<UUID> findSpecialtyIdsByRoomId(@Param("roomId") UUID roomId);
}

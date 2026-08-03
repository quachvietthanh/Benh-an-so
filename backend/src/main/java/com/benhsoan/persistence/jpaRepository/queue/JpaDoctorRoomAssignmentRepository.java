package com.benhsoan.persistence.jpaRepository.queue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.benhsoan.persistence.entity.queue.DoctorRoomAssignmentEntity;

public interface JpaDoctorRoomAssignmentRepository extends JpaRepository<DoctorRoomAssignmentEntity, UUID> {
    Optional<DoctorRoomAssignmentEntity> findByDoctorId(UUID doctorId);

    Optional<DoctorRoomAssignmentEntity> findByRoomId(UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from DoctorRoomAssignmentEntity assignment where assignment.doctorId = :doctorId")
    Optional<DoctorRoomAssignmentEntity> findByDoctorIdForUpdate(@Param("doctorId") UUID doctorId);

    void deleteByDoctorId(UUID doctorId);
}

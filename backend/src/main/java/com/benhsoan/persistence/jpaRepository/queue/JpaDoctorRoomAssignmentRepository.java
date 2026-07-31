package com.benhsoan.persistence.jpaRepository.queue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.queue.DoctorRoomAssignmentEntity;

public interface JpaDoctorRoomAssignmentRepository extends JpaRepository<DoctorRoomAssignmentEntity, UUID> { Optional<DoctorRoomAssignmentEntity> findByDoctorId(UUID doctorId); }

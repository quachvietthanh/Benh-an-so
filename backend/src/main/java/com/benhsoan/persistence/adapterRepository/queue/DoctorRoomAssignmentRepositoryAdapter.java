package com.benhsoan.persistence.adapterRepository.queue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.queue.DoctorRoomAssignment;
import com.benhsoan.persistence.jpaRepository.queue.JpaDoctorRoomAssignmentRepository;
import com.benhsoan.persistence.mapper.queue.QueueStructurePersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.queue.DoctorRoomAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DoctorRoomAssignmentRepositoryAdapter implements DoctorRoomAssignmentRepository {
    private final JpaDoctorRoomAssignmentRepository jpaRepository;
    private final QueueStructurePersistenceMapper mapper;
    public Optional<DoctorRoomAssignment> findByDoctorId(UUID doctorId) { return jpaRepository.findByDoctorId(doctorId).map(mapper::toDomain); }
    public Optional<DoctorRoomAssignment> findByDoctorIdForUpdate(UUID doctorId) { return jpaRepository.findByDoctorIdForUpdate(doctorId).map(mapper::toDomain); }
}

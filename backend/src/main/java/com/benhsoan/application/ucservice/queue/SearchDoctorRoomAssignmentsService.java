package com.benhsoan.application.ucservice.queue;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.benhsoan.port.dto.result.DoctorRoomAssignmentResult;
import com.benhsoan.port.inbound.queue.SearchDoctorRoomAssignmentsUseCase;
import com.benhsoan.port.outbound.repository.queue.DoctorRoomAssignmentRepository;
import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class SearchDoctorRoomAssignmentsService implements SearchDoctorRoomAssignmentsUseCase {
    private final DoctorRoomAssignmentRepository repository;
    private final RoomAuthorizationService authorizationService;
    private final DoctorRoomAssignmentResultMapper mapper;
    public List<DoctorRoomAssignmentResult> search(UUID doctorId, UUID roomId) {
        authorizationService.requireManageAccess();
        if (doctorId != null) return repository.findByDoctorId(doctorId).stream().map(mapper::toResult).toList();
        if (roomId != null) return repository.findByRoomId(roomId).stream().map(mapper::toResult).toList();
        throw new com.benhsoan.domain.shared.exception.ValidationException("doctorId or roomId is required.");
    }
}

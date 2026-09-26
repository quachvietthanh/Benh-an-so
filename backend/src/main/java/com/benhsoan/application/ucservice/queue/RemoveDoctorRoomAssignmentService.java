package com.benhsoan.application.ucservice.queue;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.exception.DoctorRoomAssignmentConflictException;
import com.benhsoan.domain.queue.exception.DoctorNotAssignedToRoomException;
import com.benhsoan.port.inbound.queue.RemoveDoctorRoomAssignmentUseCase;
import com.benhsoan.port.outbound.repository.queue.DoctorRoomAssignmentRepository;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.time.ClockPort;
import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor @Transactional
public class RemoveDoctorRoomAssignmentService implements RemoveDoctorRoomAssignmentUseCase {
    private final DoctorRoomAssignmentRepository assignmentRepository; private final MedicalQueueRepository medicalQueueRepository;
    private final RoomAuthorizationService authorizationService; private final DoctorRoomAssignmentAuditService auditService; private final ClockPort clockPort;
    public void remove(UUID doctorId) {
        authorizationService.requireManageAccess();
        var assignment = assignmentRepository.findByDoctorIdForUpdate(doctorId).orElseThrow(() -> new DoctorNotAssignedToRoomException(doctorId));
        if (medicalQueueRepository.existsByDoctorIdAndQueueDateAndStatus(doctorId, clockPort.now().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate(), MedicalQueueStatus.OPEN))
            throw new DoctorRoomAssignmentConflictException("Doctor has an active queue today and cannot be unassigned.");
        assignmentRepository.deleteByDoctorId(doctorId); auditService.record(ActionType.DELETE, assignment);
    }
}

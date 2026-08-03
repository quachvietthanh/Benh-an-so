package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Component;
import com.benhsoan.domain.queue.DoctorRoomAssignment;
import com.benhsoan.port.dto.result.DoctorRoomAssignmentResult;
@Component
class DoctorRoomAssignmentResultMapper {
    DoctorRoomAssignmentResult toResult(DoctorRoomAssignment assignment) {
        return new DoctorRoomAssignmentResult(assignment.getId(), assignment.getDoctorId(), assignment.getRoomId(), assignment.getAssignedBy(), assignment.getAssignedAt());
    }
}

package com.benhsoan.adapter.inbound.rest.mapper;
import org.springframework.stereotype.Component;
import com.benhsoan.adapter.inbound.rest.request.queue.AssignDoctorRoomRequest;
import com.benhsoan.adapter.inbound.rest.response.queue.DoctorRoomAssignmentResponse;
import com.benhsoan.port.dto.command.queue.AssignDoctorRoomCommand;
import com.benhsoan.port.dto.result.DoctorRoomAssignmentResult;
import java.util.UUID;
@Component
public class DoctorRoomAssignmentRestMapper {
    public AssignDoctorRoomCommand toCommand(UUID doctorId, AssignDoctorRoomRequest request) { return new AssignDoctorRoomCommand(doctorId, request.roomId()); }
    public DoctorRoomAssignmentResponse toResponse(DoctorRoomAssignmentResult result) { return new DoctorRoomAssignmentResponse(result.id(), result.doctorId(), result.roomId(), result.assignedBy(), result.assignedAt()); }
}

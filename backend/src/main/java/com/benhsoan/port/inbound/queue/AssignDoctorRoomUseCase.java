package com.benhsoan.port.inbound.queue;
import com.benhsoan.port.dto.command.queue.AssignDoctorRoomCommand;
import com.benhsoan.port.dto.result.DoctorRoomAssignmentResult;
public interface AssignDoctorRoomUseCase { DoctorRoomAssignmentResult assign(AssignDoctorRoomCommand command); }

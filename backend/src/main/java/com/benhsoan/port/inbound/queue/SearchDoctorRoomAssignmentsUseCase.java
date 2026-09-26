package com.benhsoan.port.inbound.queue;

import java.util.List;
import java.util.UUID;
import com.benhsoan.port.dto.result.DoctorRoomAssignmentResult;
public interface SearchDoctorRoomAssignmentsUseCase { List<DoctorRoomAssignmentResult> search(UUID doctorId, UUID roomId); }

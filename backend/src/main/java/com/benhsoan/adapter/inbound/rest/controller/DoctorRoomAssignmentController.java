package com.benhsoan.adapter.inbound.rest.controller;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.benhsoan.adapter.inbound.rest.mapper.DoctorRoomAssignmentRestMapper;
import com.benhsoan.adapter.inbound.rest.request.queue.AssignDoctorRoomRequest;
import com.benhsoan.adapter.inbound.rest.response.queue.DoctorRoomAssignmentResponse;
import com.benhsoan.port.inbound.queue.AssignDoctorRoomUseCase;
import com.benhsoan.port.inbound.queue.RemoveDoctorRoomAssignmentUseCase;
import com.benhsoan.port.inbound.queue.SearchDoctorRoomAssignmentsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@RestController @RequestMapping @RequiredArgsConstructor
public class DoctorRoomAssignmentController {
    private final SearchDoctorRoomAssignmentsUseCase searchUseCase; private final AssignDoctorRoomUseCase assignUseCase;
    private final RemoveDoctorRoomAssignmentUseCase removeUseCase; private final DoctorRoomAssignmentRestMapper mapper;
    @GetMapping("/doctor-room-assignments") @PreAuthorize("hasRole('ADMIN')")
    public List<DoctorRoomAssignmentResponse> search(@RequestParam(required = false) UUID doctorId, @RequestParam(required = false) UUID roomId) {
        return searchUseCase.search(doctorId, roomId).stream().map(mapper::toResponse).toList();
    }
    @PutMapping("/doctors/{doctorId}/room-assignment") @PreAuthorize("hasRole('ADMIN')")
    public DoctorRoomAssignmentResponse assign(@PathVariable UUID doctorId, @Valid @RequestBody AssignDoctorRoomRequest request) {
        return mapper.toResponse(assignUseCase.assign(mapper.toCommand(doctorId, request)));
    }
    @DeleteMapping("/doctors/{doctorId}/room-assignment") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')")
    public void remove(@PathVariable UUID doctorId) { removeUseCase.remove(doctorId); }
}

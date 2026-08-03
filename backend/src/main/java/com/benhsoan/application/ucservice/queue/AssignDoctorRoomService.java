package com.benhsoan.application.ucservice.queue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.domain.queue.DoctorRoomAssignment;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.exception.DoctorRoomAssignmentConflictException;
import com.benhsoan.domain.queue.exception.RoomNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.queue.AssignDoctorRoomCommand;
import com.benhsoan.port.dto.result.DoctorRoomAssignmentResult;
import com.benhsoan.port.inbound.queue.AssignDoctorRoomUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.DoctorRoomAssignmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.RoomRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor @Transactional
public class AssignDoctorRoomService implements AssignDoctorRoomUseCase {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final DoctorRoomAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoomRepository roomRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final RoomAuthorizationService authorizationService;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final DoctorRoomAssignmentResultMapper resultMapper;
    private final DoctorRoomAssignmentAuditService auditService;
    public DoctorRoomAssignmentResult assign(AssignDoctorRoomCommand command) {
        authorizationService.requireManageAccess();
        validateDoctor(command.doctorId());
        roomRepository.findActiveById(command.roomId()).orElseThrow(() -> new RoomNotFoundException(command.roomId()));
        DoctorRoomAssignment existingForDoctor = assignmentRepository.findByDoctorIdForUpdate(command.doctorId()).orElse(null);
        assignmentRepository.findByRoomId(command.roomId()).filter(item -> !item.getDoctorId().equals(command.doctorId()))
                .ifPresent(item -> { throw new DoctorRoomAssignmentConflictException("Room is already assigned to another doctor."); });
        if (existingForDoctor != null && medicalQueueRepository.existsByDoctorIdAndQueueDateAndStatus(command.doctorId(), today(), MedicalQueueStatus.OPEN)) {
            throw new DoctorRoomAssignmentConflictException("Doctor has an active queue today and cannot be reassigned.");
        }
        DoctorRoomAssignment assignment = existingForDoctor == null
                ? DoctorRoomAssignment.create(command.doctorId(), command.roomId(), currentUserPort.getCurrentUserId(), clockPort.now())
                : existingForDoctor.reassign(command.roomId(), currentUserPort.getCurrentUserId(), clockPort.now());
        DoctorRoomAssignment saved = assignmentRepository.save(assignment);
        auditService.record(existingForDoctor == null ? ActionType.CREATE : ActionType.UPDATE, saved);
        return resultMapper.toResult(saved);
    }
    private void validateDoctor(UUID doctorId) {
        var doctor = userRepository.findById(doctorId).orElseThrow(UserNotFoundException::new);
        if (!doctor.isActive()) throw new DoctorRoomAssignmentConflictException("Doctor account is inactive.");
        UUID doctorRoleId = roleRepository.findByName("DOCTOR").orElseThrow(() -> new ValidationException("DOCTOR role is not configured.")).getId();
        if (!doctorRoleId.equals(doctor.getRoleId())) throw new DoctorRoomAssignmentConflictException("User must have the DOCTOR role.");
    }
    private LocalDate today() { return clockPort.now().atZone(CLINIC_ZONE).toLocalDate(); }
}

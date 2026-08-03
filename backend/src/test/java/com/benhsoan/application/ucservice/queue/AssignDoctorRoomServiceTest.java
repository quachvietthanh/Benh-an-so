package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.queue.DoctorRoomAssignment;
import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.exception.DoctorRoomAssignmentConflictException;
import com.benhsoan.port.dto.command.queue.AssignDoctorRoomCommand;
import com.benhsoan.port.outbound.repository.crudRepository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.DoctorRoomAssignmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.RoomRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class AssignDoctorRoomServiceTest {
    @Mock private DoctorRoomAssignmentRepository assignmentRepository; @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository; @Mock private RoomRepository roomRepository; @Mock private MedicalQueueRepository medicalQueueRepository;
    @Mock private RoomAuthorizationService authorizationService; @Mock private CurrentUserPort currentUserPort; @Mock private ClockPort clockPort;
    @Mock private DoctorRoomAssignmentAuditService auditService; @Spy private DoctorRoomAssignmentResultMapper resultMapper = new DoctorRoomAssignmentResultMapper();
    @InjectMocks private AssignDoctorRoomService service;

    @Test
    void assignsActiveDoctorToAvailableActiveRoom() {
        UUID doctorId = UUID.randomUUID(), roomId = UUID.randomUUID(), roleId = UUID.randomUUID(), actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(User.restore(doctorId, "doctor", "hash", "Doctor", "doctor@test", null, roleId, true, null, now)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(Role.restore(roleId, "DOCTOR", null, true, now, now, Set.of())));
        when(roomRepository.findActiveById(roomId)).thenReturn(Optional.of(Room.restore(roomId, "P101", "Room", true, now, null)));
        when(assignmentRepository.findByDoctorIdForUpdate(doctorId)).thenReturn(Optional.empty());
        when(assignmentRepository.findByRoomId(roomId)).thenReturn(Optional.empty());
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId); when(clockPort.now()).thenReturn(now);
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.assign(new AssignDoctorRoomCommand(doctorId, roomId));
        assertEquals(doctorId, result.doctorId()); assertEquals(roomId, result.roomId()); verify(auditService).record(any(), any());
    }

    @Test
    void rejectsReassignWhenDoctorHasOpenQueueToday() {
        UUID doctorId = UUID.randomUUID(), roomId = UUID.randomUUID(), oldRoomId = UUID.randomUUID(), roleId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(User.restore(doctorId, "doctor", "hash", "Doctor", "doctor@test", null, roleId, true, null, now)));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(Role.restore(roleId, "DOCTOR", null, true, now, now, Set.of())));
        when(roomRepository.findActiveById(roomId)).thenReturn(Optional.of(Room.restore(roomId, "P101", "Room", true, now, null)));
        when(assignmentRepository.findByDoctorIdForUpdate(doctorId)).thenReturn(Optional.of(DoctorRoomAssignment.create(doctorId, oldRoomId, UUID.randomUUID(), now)));
        when(assignmentRepository.findByRoomId(roomId)).thenReturn(Optional.empty()); when(clockPort.now()).thenReturn(now);
        when(medicalQueueRepository.existsByDoctorIdAndQueueDateAndStatus(any(), any(), org.mockito.ArgumentMatchers.eq(MedicalQueueStatus.OPEN))).thenReturn(true);
        assertThrows(DoctorRoomAssignmentConflictException.class, () -> service.assign(new AssignDoctorRoomCommand(doctorId, roomId)));
    }
}

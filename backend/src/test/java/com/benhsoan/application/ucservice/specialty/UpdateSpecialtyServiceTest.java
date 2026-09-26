package com.benhsoan.application.ucservice.specialty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyNameAlreadyExistsException;
import com.benhsoan.port.dto.command.specialty.UpdateSpecialtyCommand;
import com.benhsoan.port.dto.result.specialty.SpecialtyDetailResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.repository.specialty.DoctorSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.RoomSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class UpdateSpecialtyServiceTest {

    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private DoctorSpecialtyRepository doctorSpecialtyRepository;
    @Mock private RoomSpecialtyRepository roomSpecialtyRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ClockPort clockPort;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private SpecialtyAuditService specialtyAuditService;
    @Mock private GetSpecialtyDetailService getSpecialtyDetailService;

    private UpdateSpecialtyService service;
    private final Instant now = Instant.parse("2026-09-18T10:00:00Z");
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UpdateSpecialtyService(
                specialtyRepository,
                doctorSpecialtyRepository,
                roomSpecialtyRepository,
                userRepository,
                roomRepository,
                clockPort,
                currentUserPort,
                specialtyAuditService,
                getSpecialtyDetailService
        );
    }

    @Test
    void updatesSpecialtyAndResyncsDoctorsAndRoomsSuccessfully() {
        UUID specialtyId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Specialty specialty = Specialty.restore(specialtyId, "PEDIATRICS", "Khoa Nhi", true, now, now);
        User doctor = User.restore(doctorId, "dr_a", "hash", "Dr A", "a@c.com", "0901", RoleConstants.DOCTOR, true, now, now);
        Room room = Room.restore(roomId, "R101", "Room 101", true, now, now);

        UpdateSpecialtyCommand command = UpdateSpecialtyCommand.builder()
                .id(specialtyId)
                .name("Khoa Nhi Tổng Hợp")
                .description("Mô tả mới")
                .doctorIds(List.of(doctorId))
                .roomIds(List.of(roomId))
                .build();

        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(specialtyRepository.existsByNameKeyAndIdNot("khoa nhi tổng hợp", specialtyId)).thenReturn(false);
        when(userRepository.findAllById(List.of(doctorId))).thenReturn(List.of(doctor));
        when(roomRepository.findActiveById(roomId)).thenReturn(Optional.of(room));
        when(clockPort.now()).thenReturn(now.plusSeconds(3600));
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);

        SpecialtyDetailResult detailResult = SpecialtyDetailResult.builder()
                .id(specialtyId)
                .code("PEDIATRICS")
                .name("Khoa Nhi Tổng Hợp")
                .active(true)
                .build();
        when(getSpecialtyDetailService.getById(specialtyId)).thenReturn(detailResult);

        SpecialtyDetailResult result = service.update(command);

        assertNotNull(result);
        assertEquals("Khoa Nhi Tổng Hợp", result.name());
        verify(doctorSpecialtyRepository).removeAssignmentsBySpecialtyId(specialtyId);
        verify(doctorSpecialtyRepository).assignDoctors(eq(specialtyId), eq(List.of(doctorId)), eq(adminId), any());
        verify(roomSpecialtyRepository).removeAssignmentsBySpecialtyId(specialtyId);
        verify(roomSpecialtyRepository).assignRooms(eq(specialtyId), eq(List.of(roomId)), any());
        verify(specialtyAuditService).record(eq(ActionType.UPDATE), any(Specialty.class), eq(List.of(doctorId)), eq(List.of(roomId)));
    }

    @Test
    void rejectsUpdateWithDuplicateName() {
        UUID specialtyId = UUID.randomUUID();
        Specialty specialty = Specialty.restore(specialtyId, "PEDIATRICS", "Khoa Nhi", true, now, now);

        UpdateSpecialtyCommand command = UpdateSpecialtyCommand.builder()
                .id(specialtyId)
                .name("Nội Khoa")
                .build();

        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(specialtyRepository.existsByNameKeyAndIdNot("nội khoa", specialtyId)).thenReturn(true);

        assertThrows(SpecialtyNameAlreadyExistsException.class, () -> service.update(command));
    }

    @Test
    void updatesSpecialtyAndPreservesAssignmentsInAuditWhenDoctorAndRoomIdsAreNull() {
        UUID specialtyId = UUID.randomUUID();
        UUID existingDoctorId = UUID.randomUUID();
        UUID existingRoomId = UUID.randomUUID();
        Specialty specialty = Specialty.restore(specialtyId, "PEDIATRICS", "Khoa Nhi", true, now, now);

        UpdateSpecialtyCommand command = UpdateSpecialtyCommand.builder()
                .id(specialtyId)
                .name("Khoa Nhi Mới")
                .description("Mô tả mới")
                .doctorIds(null)
                .roomIds(null)
                .build();

        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(specialtyRepository.existsByNameKeyAndIdNot("khoa nhi mới", specialtyId)).thenReturn(false);
        when(doctorSpecialtyRepository.findDoctorIdsBySpecialtyId(specialtyId)).thenReturn(List.of(existingDoctorId));
        when(roomSpecialtyRepository.findRoomIdsBySpecialtyId(specialtyId)).thenReturn(List.of(existingRoomId));
        when(clockPort.now()).thenReturn(now.plusSeconds(3600));

        SpecialtyDetailResult detailResult = SpecialtyDetailResult.builder()
                .id(specialtyId)
                .code("PEDIATRICS")
                .name("Khoa Nhi Mới")
                .active(true)
                .build();
        when(getSpecialtyDetailService.getById(specialtyId)).thenReturn(detailResult);

        SpecialtyDetailResult result = service.update(command);

        assertNotNull(result);
        verify(specialtyAuditService).record(eq(ActionType.UPDATE), any(Specialty.class), eq(List.of(existingDoctorId)), eq(List.of(existingRoomId)));
    }
}

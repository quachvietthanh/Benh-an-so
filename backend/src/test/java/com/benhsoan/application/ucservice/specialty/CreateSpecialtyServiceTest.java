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
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyCodeAlreadyExistsException;
import com.benhsoan.domain.specialty.exception.SpecialtyNameAlreadyExistsException;
import com.benhsoan.port.dto.command.specialty.CreateSpecialtyCommand;
import com.benhsoan.port.dto.result.specialty.SpecialtyDetailResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.repository.specialty.DoctorSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.RoomSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class CreateSpecialtyServiceTest {

    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private DoctorSpecialtyRepository doctorSpecialtyRepository;
    @Mock private RoomSpecialtyRepository roomSpecialtyRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ClockPort clockPort;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private SpecialtyAuditService specialtyAuditService;
    @Mock private GetSpecialtyDetailService getSpecialtyDetailService;

    private CreateSpecialtyService service;
    private final Instant now = Instant.parse("2026-09-18T10:00:00Z");
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CreateSpecialtyService(
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
    void createsSpecialtyAndAssignsDoctorsAndRoomsSuccessfully() {
        UUID doctorId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        User doctor = User.restore(doctorId, "dr_a", "hash", "Dr A", "a@c.com", "0901", RoleConstants.DOCTOR, true, now, now);
        Room room = Room.restore(roomId, "R101", "Room 101", true, now, now);

        CreateSpecialtyCommand command = CreateSpecialtyCommand.builder()
                .code("PEDIATRICS")
                .name("Khoa Nhi")
                .description("Chuyên khoa nhi")
                .doctorIds(List.of(doctorId))
                .roomIds(List.of(roomId))
                .build();

        when(specialtyRepository.existsByCode("PEDIATRICS")).thenReturn(false);
        when(specialtyRepository.existsByNameKey("khoa nhi")).thenReturn(false);
        when(userRepository.findAllById(List.of(doctorId))).thenReturn(List.of(doctor));
        when(roomRepository.findActiveById(roomId)).thenReturn(Optional.of(room));
        when(clockPort.now()).thenReturn(now);
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));

        SpecialtyDetailResult detailResult = SpecialtyDetailResult.builder()
                .id(UUID.randomUUID())
                .code("PEDIATRICS")
                .name("Khoa Nhi")
                .active(true)
                .build();
        when(getSpecialtyDetailService.getById(any(UUID.class))).thenReturn(detailResult);

        SpecialtyDetailResult result = service.create(command);

        assertNotNull(result);
        assertEquals("PEDIATRICS", result.code());
        verify(specialtyRepository).save(any(Specialty.class));
        verify(doctorSpecialtyRepository).assignDoctors(any(UUID.class), eq(List.of(doctorId)), eq(adminId), eq(now));
        verify(roomSpecialtyRepository).assignRooms(any(UUID.class), eq(List.of(roomId)), eq(now));
        verify(specialtyAuditService).record(eq(ActionType.CREATE), any(Specialty.class), eq(List.of(doctorId)), eq(List.of(roomId)));
    }

    @Test
    void rejectsDuplicateSpecialtyCode() {
        CreateSpecialtyCommand command = CreateSpecialtyCommand.builder()
                .code("PEDIATRICS")
                .name("Khoa Nhi")
                .build();
        when(specialtyRepository.existsByCode("PEDIATRICS")).thenReturn(true);

        assertThrows(SpecialtyCodeAlreadyExistsException.class, () -> service.create(command));
    }

    @Test
    void rejectsDuplicateSpecialtyName() {
        CreateSpecialtyCommand command = CreateSpecialtyCommand.builder()
                .code("PEDIATRICS")
                .name("Khoa Nhi")
                .build();
        when(specialtyRepository.existsByCode("PEDIATRICS")).thenReturn(false);
        when(specialtyRepository.existsByNameKey("khoa nhi")).thenReturn(true);

        assertThrows(SpecialtyNameAlreadyExistsException.class, () -> service.create(command));
    }

    @Test
    void rejectsNonDoctorUserAssignment() {
        UUID nonDoctorId = UUID.randomUUID();
        User nonDoctor = User.restore(nonDoctorId, "rec", "hash", "Rec", "r@c.com", "0901", RoleConstants.RECEPTIONIST, true, now, now);

        CreateSpecialtyCommand command = CreateSpecialtyCommand.builder()
                .code("PEDIATRICS")
                .name("Khoa Nhi")
                .doctorIds(List.of(nonDoctorId))
                .build();

        when(specialtyRepository.existsByCode("PEDIATRICS")).thenReturn(false);
        when(specialtyRepository.existsByNameKey("khoa nhi")).thenReturn(false);
        when(userRepository.findAllById(List.of(nonDoctorId))).thenReturn(List.of(nonDoctor));

        assertThrows(ValidationException.class, () -> service.create(command));
    }

    @Test
    void rejectsInactiveRoomAssignment() {
        UUID roomId = UUID.randomUUID();
        CreateSpecialtyCommand command = CreateSpecialtyCommand.builder()
                .code("PEDIATRICS")
                .name("Khoa Nhi")
                .roomIds(List.of(roomId))
                .build();

        when(specialtyRepository.existsByCode("PEDIATRICS")).thenReturn(false);
        when(specialtyRepository.existsByNameKey("khoa nhi")).thenReturn(false);
        when(roomRepository.findActiveById(roomId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.create(command));
    }
}

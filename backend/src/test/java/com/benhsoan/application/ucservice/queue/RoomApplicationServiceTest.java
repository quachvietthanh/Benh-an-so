package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.queue.exception.RoomCodeAlreadyExistsException;
import com.benhsoan.domain.queue.exception.RoomNotFoundException;
import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.port.dto.command.queue.CreateRoomCommand;
import com.benhsoan.port.dto.command.queue.SearchRoomsQuery;
import com.benhsoan.port.dto.command.queue.UpdateRoomCommand;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class RoomApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");

    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final RoomAuditService auditService = mock(RoomAuditService.class);
    private final RoomResultMapper resultMapper = new RoomResultMapper();
    private final RoomAuthorizationService authorizationService = new RoomAuthorizationService(currentUserPort);

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsNormalizedRoomForAdmin() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        CreateRoomService service = new CreateRoomService(
                roomRepository, authorizationService, resultMapper, clockPort, auditService);

        var result = service.create(new CreateRoomCommand(" p103 ", "Phong kham 103"));

        assertEquals("P103", result.code());
        assertTrue(result.active());
        verify(roomRepository).existsByCode("P103");
        verify(auditService).record(eq(ActionType.CREATE), any(Room.class));
    }

    @Test
    void rejectsDuplicateRoomCodeBeforeSave() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(roomRepository.existsByCode("P103")).thenReturn(true);
        CreateRoomService service = new CreateRoomService(
                roomRepository, authorizationService, resultMapper, clockPort, auditService);

        assertThrows(RoomCodeAlreadyExistsException.class,
                () -> service.create(new CreateRoomCommand("p103", "Phong kham 103")));
    }

    @Test
    void searchesRoomsForReceptionist() {
        Room room = Room.create("P101", "Phong kham 101", NOW);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(roomRepository.search(any(), any(), any())).thenReturn(new PageImpl<>(java.util.List.of(room)));
        SearchRoomsService service = new SearchRoomsService(roomRepository, authorizationService, resultMapper);

        var result = service.search(new SearchRoomsQuery("P1", true, 0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("P101", result.getContent().getFirst().code());
    }

    @Test
    void returnsNotFoundForUnknownRoom() {
        UUID roomId = UUID.randomUUID();
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        GetRoomService service = new GetRoomService(roomRepository, authorizationService, resultMapper);

        assertThrows(RoomNotFoundException.class, () -> service.getById(roomId));
    }

    @Test
    void updatesActivatesAndDeactivatesRoomForAdmin() {
        Room room = Room.create("P101", "Phong kham 101", NOW.minusSeconds(60));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        var updated = new UpdateRoomService(roomRepository, authorizationService, resultMapper, clockPort, auditService)
                .update(new UpdateRoomCommand(room.getId(), "Phong kham Noi"));
        var deactivated = new DeactivateRoomService(roomRepository, authorizationService, resultMapper, clockPort,
                auditService)
                .deactivate(room.getId());
        var activated = new ActivateRoomService(roomRepository, authorizationService, resultMapper, clockPort,
                auditService)
                .activate(room.getId());

        assertEquals("P101", updated.code());
        assertEquals("Phong kham Noi", updated.name());
        assertFalse(deactivated.active());
        assertTrue(activated.active());
        verify(auditService).record(ActionType.UPDATE, room);
        verify(auditService).record(ActionType.DEACTIVATE, room);
        verify(auditService).record(ActionType.ACTIVATE, room);
    }

    @Test
    void rejectsRoomManagementForNonAdmin() {
        CreateRoomService service = new CreateRoomService(
                roomRepository, authorizationService, resultMapper, clockPort, auditService);

        assertThrows(UnauthorizedQueueOperationException.class,
                () -> service.create(new CreateRoomCommand("P103", "Phong kham 103")));
    }
}

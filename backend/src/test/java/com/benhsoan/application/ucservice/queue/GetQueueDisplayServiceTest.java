package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.enums.QueuePriority;
import com.benhsoan.port.dto.command.queue.GetQueueDisplayQuery;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.dto.result.WaitingRoomBoardResult;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class GetQueueDisplayServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final Instant NOW = Instant.parse("2026-09-24T08:00:00Z");

    @Mock private QueueItemQueryRepository queueItemQueryRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ClockPort clockPort;

    @InjectMocks private GetQueueDisplayService service;

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void returnsEmptyBoardWhenNoQueueItemsExist() {
        when(queueItemQueryRepository.findActiveQueueBoard(TODAY, null)).thenReturn(List.of());

        WaitingRoomBoardResult board = service.getWaitingRoomDisplay(new GetQueueDisplayQuery(TODAY, null));

        assertEquals(TODAY, board.date());
        assertEquals(NOW, board.updatedAt());
        assertTrue(board.rooms().isEmpty());
    }

    @Test
    void groupsByRoomAndAbbreviatesNamesAndSortsWaitingByPriority() {
        UUID room1Id = UUID.randomUUID();
        UUID room2Id = UUID.randomUUID();
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();

        Room room1 = Room.restore(room1Id, "P101", "Phòng Khám Nội 1", true, NOW, NOW);
        Room room2 = Room.restore(room2Id, "P102", "Phòng Khám Ngoại", true, NOW, NOW);
        when(roomRepository.findAllById(any())).thenReturn(List.of(room1, room2));

        // Room 1: 1 calling (TC-01), 3 waiting with different priorities (TC-03)
        QueueItemResult callingItem = createItem(room1Id, "P101", doc1Id, "BS. Hoàng", 1, "Nguyễn Văn An",
                QueueItemStatus.IN_PROGRESS, QueuePriority.NORMAL, null, NOW.minusSeconds(300));

        QueueItemResult regularWaiting = createItem(room1Id, "P101", doc1Id, "BS. Hoàng", 2, "Trần Thị Bình",
                QueueItemStatus.WAITING, QueuePriority.NORMAL, null, null);

        QueueItemResult emergencyWaiting = createItem(room1Id, "P101", doc1Id, "BS. Hoàng", 3, "Lê Cảnh Cấp",
                QueueItemStatus.WAITING, QueuePriority.EMERGENCY, NOW.minusSeconds(60), null);

        QueueItemResult priorityWaiting = createItem(room1Id, "P101", doc1Id, "BS. Hoàng", 4, "Phạm Ưu Tiên",
                QueueItemStatus.WAITING, QueuePriority.PRIORITY, NOW.minusSeconds(120), null);

        // Room 2: 1 waiting only, no calling
        QueueItemResult room2Waiting = createItem(room2Id, "P102", doc2Id, "BS. Mai", 1, "Đỗ Thị Giang",
                QueueItemStatus.WAITING, QueuePriority.NORMAL, null, null);

        when(queueItemQueryRepository.findActiveQueueBoard(TODAY, null))
                .thenReturn(List.of(callingItem, regularWaiting, emergencyWaiting, priorityWaiting, room2Waiting));

        WaitingRoomBoardResult board = service.getWaitingRoomDisplay(new GetQueueDisplayQuery(TODAY, null));

        assertEquals(2, board.rooms().size());

        // Verify Room 1
        var r1 = board.rooms().getFirst();
        assertEquals(room1Id, r1.roomId());
        assertEquals("P101", r1.roomNumber());
        assertEquals("Phòng Khám Nội 1", r1.roomName());
        assertEquals("BS. Hoàng", r1.doctorName());

        // TC-01: In-progress patient is in currentCalling
        assertNotNull(r1.currentCalling());
        assertEquals(1, r1.currentCalling().queueNumber());
        assertEquals(QueueItemStatus.IN_PROGRESS, r1.currentCalling().status());
        // TC-02: Name is abbreviated (N. V. A)
        assertEquals("N. V. A", r1.currentCalling().patientInitials());

        // TC-03: Waiting list sorted by EMERGENCY (rank 1) -> PRIORITY (rank 2) -> NORMAL (rank 3)
        assertEquals(3, r1.waitingList().size());
        assertEquals(3, r1.waitingList().get(0).queueNumber());
        assertEquals(QueuePriority.EMERGENCY, r1.waitingList().get(0).priority());
        assertEquals("L. C. C", r1.waitingList().get(0).patientInitials());

        assertEquals(4, r1.waitingList().get(1).queueNumber());
        assertEquals(QueuePriority.PRIORITY, r1.waitingList().get(1).priority());
        assertEquals("P. Ư. T", r1.waitingList().get(1).patientInitials());

        assertEquals(2, r1.waitingList().get(2).queueNumber());
        assertEquals(QueuePriority.NORMAL, r1.waitingList().get(2).priority());
        assertEquals("T. T. B", r1.waitingList().get(2).patientInitials());

        // Verify Room 2
        var r2 = board.rooms().get(1);
        assertEquals(room2Id, r2.roomId());
        assertNull(r2.currentCalling());
        assertEquals(1, r2.waitingList().size());
        assertEquals(1, r2.waitingList().getFirst().queueNumber());
        assertEquals("Đ. T. G", r2.waitingList().getFirst().patientInitials());

        // Verify N+1 query elimination (TEST-02)
        verify(roomRepository, times(1)).findAllById(any());
        verify(roomRepository, never()).findById(any());
    }

    @Test
    void limitsWaitingListToTenItemsWhenMorePatientsWait() {
        UUID roomId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Room room = Room.restore(roomId, "P105", "Phòng Khám Tai Mũi Họng", true, NOW, NOW);
        when(roomRepository.findAllById(any())).thenReturn(List.of(room));

        // Create 15 waiting patients (TEST-01)
        List<QueueItemResult> items = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            items.add(createItem(roomId, "P105", doctorId, "BS. Tuấn", i, "Bệnh Nhân " + i,
                    QueueItemStatus.WAITING, QueuePriority.NORMAL, null, null));
        }

        when(queueItemQueryRepository.findActiveQueueBoard(TODAY, roomId)).thenReturn(items);

        WaitingRoomBoardResult board = service.getWaitingRoomDisplay(new GetQueueDisplayQuery(TODAY, roomId));

        assertEquals(1, board.rooms().size());
        var r = board.rooms().getFirst();
        assertNull(r.currentCalling());
        assertEquals(10, r.waitingList().size());
        assertEquals(1, r.waitingList().get(0).queueNumber());
        assertEquals(10, r.waitingList().get(9).queueNumber());
    }

    @Test
    void returnsEmptyWaitingRoomWhenFilteredByRoomIdWithNoPatients() {
        UUID emptyRoomId = UUID.randomUUID();
        Room room = Room.restore(emptyRoomId, "P108", "Phòng Khám Da Liễu", true, NOW, NOW);
        when(queueItemQueryRepository.findActiveQueueBoard(TODAY, emptyRoomId)).thenReturn(List.of());
        when(roomRepository.findById(emptyRoomId)).thenReturn(Optional.of(room));

        WaitingRoomBoardResult board = service.getWaitingRoomDisplay(new GetQueueDisplayQuery(TODAY, emptyRoomId));

        assertEquals(1, board.rooms().size());
        var r = board.rooms().getFirst();
        assertEquals(emptyRoomId, r.roomId());
        assertEquals("P108", r.roomNumber());
        assertEquals("Phòng Khám Da Liễu", r.roomName());
        assertNull(r.currentCalling());
        assertTrue(r.waitingList().isEmpty());
    }

    private QueueItemResult createItem(UUID roomId, String roomNumber, UUID doctorId, String doctorName,
            int queueNumber, String patientName, QueueItemStatus status, QueuePriority priority,
            Instant prioritizedAt, Instant calledAt) {
        return new QueueItemResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "BN" + queueNumber, patientName,
                doctorId, doctorName, roomId, roomNumber, null, UUID.randomUUID(), "VIS" + queueNumber,
                QueueItemSourceType.WALK_IN, status, queueNumber, TODAY,
                NOW, calledAt, null, null, null, null, null, 1,
                priority, "Reason", prioritizedAt, UUID.randomUUID()
        );
    }
}

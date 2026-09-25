package com.benhsoan.application.ucservice.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.PatientAnonymizer;
import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.enums.QueuePriority;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.queue.GetQueueDisplayQuery;
import com.benhsoan.port.dto.result.QueueDisplayItemResult;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.dto.result.RoomQueueDisplayResult;
import com.benhsoan.port.dto.result.WaitingRoomBoardResult;
import com.benhsoan.port.inbound.queue.GetQueueDisplayUseCase;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetQueueDisplayService implements GetQueueDisplayUseCase {

    private static final int MAX_WAITING_ITEMS_PER_ROOM = 10;
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final QueueItemQueryRepository queueItemQueryRepository;
    private final RoomRepository roomRepository;
    private final ClockPort clockPort;

    @Override
    public WaitingRoomBoardResult getWaitingRoomDisplay(GetQueueDisplayQuery query) {
        if (query == null) {
            throw new ValidationException("Query cannot be null.");
        }

        LocalDate targetDate = query.queueDate() != null
                ? query.queueDate()
                : clockPort.now().atZone(CLINIC_ZONE).toLocalDate();

        List<QueueItemResult> items = queueItemQueryRepository.findActiveQueueBoard(targetDate, query.roomId());
        Instant now = clockPort.now();

        if (items.isEmpty()) {
            if (query.roomId() != null) {
                return roomRepository.findById(query.roomId())
                        .map(room -> new WaitingRoomBoardResult(
                                targetDate,
                                now,
                                List.of(new RoomQueueDisplayResult(
                                        room.getId(),
                                        room.getCode(),
                                        room.getName(),
                                        null,
                                        null,
                                        null,
                                        List.of()
                                ))
                        ))
                        .orElseGet(() -> new WaitingRoomBoardResult(targetDate, now, List.of()));
            }
            return new WaitingRoomBoardResult(targetDate, now, List.of());
        }

        // Group items by roomId preserving order of rooms
        Map<UUID, List<QueueItemResult>> itemsByRoom = items.stream()
                .filter(item -> item.roomId() != null)
                .collect(Collectors.groupingBy(
                        QueueItemResult::roomId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Set<UUID> allRoomIds = new LinkedHashSet<>(itemsByRoom.keySet());
        if (query.roomId() != null) {
            allRoomIds.add(query.roomId());
        }

        Map<UUID, Room> roomMap = roomRepository.findAllById(allRoomIds).stream()
                .collect(Collectors.toMap(Room::getId, r -> r, (r1, r2) -> r1));

        List<RoomQueueDisplayResult> roomResults = new ArrayList<>();

        for (Map.Entry<UUID, List<QueueItemResult>> entry : itemsByRoom.entrySet()) {
            UUID roomId = entry.getKey();
            List<QueueItemResult> roomItems = entry.getValue();

            QueueItemResult sample = roomItems.getFirst();
            String roomNumber = sample.roomNumber();
            UUID doctorId = sample.doctorId();
            String doctorName = sample.doctorName();

            String roomName = Optional.ofNullable(roomMap.get(roomId))
                    .map(Room::getName)
                    .orElse(roomNumber);

            // Find current calling item (IN_PROGRESS)
            QueueDisplayItemResult currentCalling = roomItems.stream()
                    .filter(item -> item.status() == QueueItemStatus.IN_PROGRESS)
                    .max(Comparator.comparing(item -> item.calledAt() != null ? item.calledAt() : Instant.MIN))
                    .map(this::toDisplayItem)
                    .orElse(null);

            // Find waiting items (WAITING) sorted by priority then queue number
            List<QueueDisplayItemResult> waitingList = roomItems.stream()
                    .filter(item -> item.status() == QueueItemStatus.WAITING)
                    .sorted(Comparator
                            .comparingInt((QueueItemResult item) -> getPriorityRank(item.priority()))
                            .thenComparing(item -> item.prioritizedAt() != null ? item.prioritizedAt() : Instant.MAX)
                            .thenComparingInt(QueueItemResult::queueNumber))
                    .limit(MAX_WAITING_ITEMS_PER_ROOM)
                    .map(this::toDisplayItem)
                    .toList();

            roomResults.add(new RoomQueueDisplayResult(
                    roomId,
                    roomNumber,
                    roomName,
                    doctorId,
                    doctorName,
                    currentCalling,
                    waitingList
            ));
        }

        if (query.roomId() != null && !itemsByRoom.containsKey(query.roomId())) {
            Room room = roomMap.get(query.roomId());
            if (room != null) {
                roomResults.add(new RoomQueueDisplayResult(
                        room.getId(),
                        room.getCode(),
                        room.getName(),
                        null,
                        null,
                        null,
                        List.of()
                ));
            }
        }

        return new WaitingRoomBoardResult(targetDate, now, roomResults);
    }

    private QueueDisplayItemResult toDisplayItem(QueueItemResult item) {
        return new QueueDisplayItemResult(
                item.id(),
                item.queueNumber(),
                PatientAnonymizer.abbreviateName(item.patientName()),
                item.status(),
                item.priority() != null ? item.priority() : QueuePriority.NORMAL,
                item.calledAt()
        );
    }

    private int getPriorityRank(QueuePriority priority) {
        if (priority == null) {
            return 3;
        }
        return switch (priority) {
            case EMERGENCY -> 1;
            case PRIORITY -> 2;
            case NORMAL -> 3;
        };
    }
}

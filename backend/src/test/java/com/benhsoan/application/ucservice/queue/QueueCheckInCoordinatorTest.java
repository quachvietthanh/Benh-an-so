package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.queue.DoctorRoomAssignment;
import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.QueueCheckInResult;
import com.benhsoan.port.outbound.generator.VisitCodeGenerator;
import com.benhsoan.port.outbound.repository.crudRepository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.DoctorRoomAssignmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.RoomRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.logRepository.AuditLogRepository;

class QueueCheckInCoordinatorTest {

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final DoctorRoomAssignmentRepository assignmentRepository = mock(DoctorRoomAssignmentRepository.class);
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
    private final QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final VisitCodeGenerator visitCodeGenerator = mock(VisitCodeGenerator.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

    private QueueCheckInCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new QueueCheckInCoordinator(patientRepository, assignmentRepository, roomRepository,
                medicalQueueRepository, queueItemRepository, visitRepository, visitCodeGenerator, auditLogRepository);
    }

    @Test
    void createsWaitingVisitAndQueueItemForWalkIn() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-31T02:00:00Z");
        MedicalQueue queue = MedicalQueue.create(doctorId, roomId, LocalDate.of(2026, 7, 31), now);

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(mock(Patient.class)));
        when(visitRepository.existsByPatientIdAndStatusIn(any(), any())).thenReturn(false);
        when(queueItemRepository.existsByPatientIdAndQueueDateAndStatusIn(any(), any(), any())).thenReturn(false);
        when(assignmentRepository.findByDoctorIdForUpdate(doctorId)).thenReturn(Optional.of(
                DoctorRoomAssignment.restore(UUID.randomUUID(), doctorId, roomId, actorId, now)));
        when(roomRepository.findActiveById(roomId)).thenReturn(Optional.of(
                Room.restore(roomId, "P101", "Phong 101", true, now, now)));
        when(medicalQueueRepository.findByDoctorIdAndQueueDateForUpdate(doctorId, LocalDate.of(2026, 7, 31)))
                .thenReturn(Optional.of(queue));
        when(visitCodeGenerator.generate()).thenReturn("VIS000003");
        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queueItemRepository.findMaxQueueNumber(queue.getId())).thenReturn(4);
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QueueCheckInResult result = coordinator.checkIn(patientId, doctorId, null, QueueItemSourceType.WALK_IN,
                "Kham tong quat", null, actorId, now);

        assertEquals(5, result.queueNumber());
        assertEquals(QueueItemSourceType.WALK_IN, result.sourceType());
        assertEquals("VIS000003", result.visitCode());
        verify(visitRepository, times(2)).save(any(Visit.class));
        verify(queueItemRepository).save(any(QueueItem.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void rejectsPatientWithActiveVisit() {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-31T02:00:00Z");
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(mock(Patient.class)));
        when(visitRepository.existsByPatientIdAndStatusIn(any(), any())).thenReturn(true);

        assertThrows(CheckInConflictException.class,
                () -> coordinator.checkIn(patientId, UUID.randomUUID(), null, QueueItemSourceType.WALK_IN,
                        "Kham tong quat", null, UUID.randomUUID(), now));
    }
}

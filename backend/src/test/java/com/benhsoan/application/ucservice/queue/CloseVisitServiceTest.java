package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionAlreadyDispensedException;
import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitCloseOutcome;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitInvalidStatusException;
import com.benhsoan.port.dto.command.queue.CloseVisitCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class CloseVisitServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Test
    void earlyEndsInProgressVisit() {
        TestContext ctx = context(true, null);
        QueueItemResult response = ctx.service.close(
                new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.EARLY_ENDED, "Bệnh nhân bỏ về"));

        assertEquals(VisitStatus.EARLY_ENDED, ctx.visit.getStatus());
        assertEquals("Bệnh nhân bỏ về", ctx.visit.getCloseReason());
        assertEquals(QueueItemStatus.CANCELLED, ctx.item.getStatus());
        assertEquals("Bệnh nhân bỏ về", ctx.item.getCancelReason());
        verify(ctx.auditService).recordEarlyEnded(ctx.item, "Bệnh nhân bỏ về");
        verify(ctx.visitRepository).save(ctx.visit);
        verify(ctx.queueItemRepository).save(ctx.item);
        assertEquals("VIS900001", response.visitCode());
    }

    @Test
    void cancelsInProgressVisit() {
        TestContext ctx = context(true, null);
        ctx.service.close(new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "Nhập nhầm lượt khám"));

        assertEquals(VisitStatus.CANCELLED, ctx.visit.getStatus());
        assertEquals("Nhập nhầm lượt khám", ctx.visit.getCloseReason());
        assertEquals(QueueItemStatus.CANCELLED, ctx.item.getStatus());
        verify(ctx.auditService).recordCancelled(ctx.item, "Nhập nhầm lượt khám");
    }

    @Test
    void rejectsWhenVisitNotInProgress() {
        TestContext ctx = context(true, null);
        ctx.visit.revertToWaiting(NOW.plusSeconds(30));
        assertThrows(VisitInvalidStatusException.class, () -> ctx.service.close(
                new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.EARLY_ENDED, "reason")));
    }

    @Test
    void rejectsWhenMedicalRecordSigned() {
        TestContext ctx = context(true, mock(MedicalRecord.class));
        when(ctx.medicalRecord.isContentLocked()).thenReturn(true);
        MedicalRecordAlreadyLockedException ex = assertThrows(MedicalRecordAlreadyLockedException.class,
                () -> ctx.service.close(
                        new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.EARLY_ENDED, "reason")));
        assertTrue(ex.getMessage().contains("QTN-18"));
        assertTrue(ex.getMessage().contains("đính chính"));
    }

    @Test
    void rejectsWhenMedicalRecordLocked() {
        TestContext ctx = context(true, mock(MedicalRecord.class));
        when(ctx.medicalRecord.isContentLocked()).thenReturn(true);
        MedicalRecordAlreadyLockedException ex = assertThrows(MedicalRecordAlreadyLockedException.class,
                () -> ctx.service.close(
                        new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "reason")));
        assertTrue(ex.getMessage().contains("QTN-18"));
        assertTrue(ex.getMessage().contains("đính chính"));
    }

    @Test
    void rejectsUnauthorizedDoctor() {
        TestContext ctx = context(true, null);
        when(ctx.currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(ctx.currentUserPort.hasRole("DOCTOR")).thenReturn(false);
        assertThrows(UnauthorizedQueueOperationException.class, () -> ctx.service.close(
                new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "reason")));
    }

    @Test
    void rejectsBlankReason() {
        TestContext ctx = context(true, null);
        assertThrows(ValidationException.class, () -> ctx.service.close(
                new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "   ")));
    }

    @Test
    void rejectsNullOutcome() {
        TestContext ctx = context(true, null);
        assertThrows(ValidationException.class, () -> ctx.service.close(
                new CloseVisitCommand(ctx.item.getId(), null, "reason")));
    }

    @Test
    void succeedsWithoutPrescriptions() {
        TestContext ctx = context(true, mock(MedicalRecord.class));
        ctx.service.close(new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.EARLY_ENDED, "reason"));
        verify(ctx.prescriptionRepository).findByMedicalRecordIdAndStatusForUpdate(
                ctx.medicalRecordId, PrescriptionStatus.PENDING_DISPENSE);
    }

    @Test
    void cancelsPendingPrescription() {
        TestContext ctx = context(true, mock(MedicalRecord.class));
        Prescription pending = pendingPrescription(UUID.randomUUID(), ctx.medicalRecordId, ctx.doctorId);
        when(ctx.prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(
                ctx.medicalRecordId, PrescriptionStatus.PENDING_DISPENSE)).thenReturn(List.of(pending));

        ctx.service.close(new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "reason"));

        assertEquals(PrescriptionStatus.CANCELLED, pending.getStatus());
        assertEquals("reason", pending.getCancelReason());
        verify(ctx.prescriptionRepository).save(pending);
        verify(ctx.auditLogRepository).save(any());
    }

    @Test
    void cancelsMultiplePendingPrescriptions() {
        TestContext ctx = context(true, mock(MedicalRecord.class));
        Prescription first = pendingPrescription(UUID.randomUUID(), ctx.medicalRecordId, ctx.doctorId);
        Prescription second = pendingPrescription(UUID.randomUUID(), ctx.medicalRecordId, ctx.doctorId);
        when(ctx.prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(
                ctx.medicalRecordId, PrescriptionStatus.PENDING_DISPENSE)).thenReturn(List.of(first, second));

        ctx.service.close(new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "reason"));

        assertEquals(PrescriptionStatus.CANCELLED, first.getStatus());
        assertEquals(PrescriptionStatus.CANCELLED, second.getStatus());
    }

    @Test
    void cancelsLinkedAppointment() {
        TestContext ctx = context(true, null);
        ctx.service.close(new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "reason"));

        assertEquals(AppointmentStatus.CANCELLED, ctx.appointment.getStatus());
        verify(ctx.appointmentRepository).save(ctx.appointment);
    }

    @Test
    void skipsAppointmentForWalkIn() {
        TestContext ctx = context(false, null);
        ctx.service.close(new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.EARLY_ENDED, "reason"));
        verify(ctx.appointmentRepository, never()).findByIdForUpdate(any());
        verify(ctx.appointmentRepository, never()).save(any());
    }

    @Test
    void rejectsReasonExceeding500Characters() {
        TestContext ctx = context(true, null);
        assertThrows(ValidationException.class, () -> ctx.service.close(
                new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "x".repeat(501))));
    }

    @Test
    void cancelledRejectsWhenDispensedPrescriptionExists() {
        TestContext ctx = context(true, mock(MedicalRecord.class));
        Prescription dispensed = dispensedPrescription(UUID.randomUUID(), ctx.medicalRecordId, ctx.doctorId);
        when(ctx.prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(
                ctx.medicalRecordId, PrescriptionStatus.DISPENSED)).thenReturn(List.of(dispensed));

        assertThrows(PrescriptionAlreadyDispensedException.class, () -> ctx.service.close(
                new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "reason")));

        // No close-flow state changed.
        assertEquals(VisitStatus.IN_PROGRESS, ctx.visit.getStatus());
        assertEquals(QueueItemStatus.IN_PROGRESS, ctx.item.getStatus());
        assertEquals(AppointmentStatus.IN_PROGRESS, ctx.appointment.getStatus());
        assertEquals(PrescriptionStatus.DISPENSED, dispensed.getStatus());
        verify(ctx.visitRepository, never()).save(ctx.visit);
        verify(ctx.queueItemRepository, never()).save(ctx.item);
        verify(ctx.appointmentRepository, never()).save(ctx.appointment);
        verify(ctx.prescriptionRepository, never()).save(any(Prescription.class));
        verify(ctx.auditLogRepository, never()).save(any());
    }

    @Test
    void cancelledRejectsWhenDispensedAndPendingPrescriptionsExist() {
        TestContext ctx = context(true, mock(MedicalRecord.class));
        Prescription dispensed = dispensedPrescription(UUID.randomUUID(), ctx.medicalRecordId, ctx.doctorId);
        Prescription pending = pendingPrescription(UUID.randomUUID(), ctx.medicalRecordId, ctx.doctorId);
        when(ctx.prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(
                ctx.medicalRecordId, PrescriptionStatus.DISPENSED)).thenReturn(List.of(dispensed));
        when(ctx.prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(
                ctx.medicalRecordId, PrescriptionStatus.PENDING_DISPENSE)).thenReturn(List.of(pending));

        assertThrows(PrescriptionAlreadyDispensedException.class, () -> ctx.service.close(
                new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "reason")));

        // PENDING_DISPENSE remains unchanged and no state was mutated.
        assertEquals(PrescriptionStatus.PENDING_DISPENSE, pending.getStatus());
        assertEquals(VisitStatus.IN_PROGRESS, ctx.visit.getStatus());
        assertEquals(QueueItemStatus.IN_PROGRESS, ctx.item.getStatus());
        assertEquals(AppointmentStatus.IN_PROGRESS, ctx.appointment.getStatus());
        verify(ctx.prescriptionRepository, never()).save(any(Prescription.class));
        verify(ctx.visitRepository, never()).save(ctx.visit);
        verify(ctx.queueItemRepository, never()).save(ctx.item);
        verify(ctx.auditLogRepository, never()).save(any());
    }

    @Test
    void cancelledSucceedsWithOnlyPendingPrescription() {
        TestContext ctx = context(true, mock(MedicalRecord.class));
        Prescription pending = pendingPrescription(UUID.randomUUID(), ctx.medicalRecordId, ctx.doctorId);
        when(ctx.prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(
                ctx.medicalRecordId, PrescriptionStatus.PENDING_DISPENSE)).thenReturn(List.of(pending));

        ctx.service.close(new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, "reason"));

        assertEquals(PrescriptionStatus.CANCELLED, pending.getStatus());
        assertEquals(VisitStatus.CANCELLED, ctx.visit.getStatus());
        assertEquals(QueueItemStatus.CANCELLED, ctx.item.getStatus());
        verify(ctx.prescriptionRepository).save(pending);
    }

    @Test
    void earlyEndedWithDispensedPrescriptionStillSucceeds() {
        TestContext ctx = context(true, mock(MedicalRecord.class));
        Prescription dispensed = dispensedPrescription(UUID.randomUUID(), ctx.medicalRecordId, ctx.doctorId);
        when(ctx.prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(
                ctx.medicalRecordId, PrescriptionStatus.DISPENSED)).thenReturn(List.of(dispensed));

        ctx.service.close(new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.EARLY_ENDED, "reason"));

        assertEquals(VisitStatus.EARLY_ENDED, ctx.visit.getStatus());
        assertEquals(QueueItemStatus.CANCELLED, ctx.item.getStatus());
        // DISPENSED prescription is untouched.
        assertEquals(PrescriptionStatus.DISPENSED, dispensed.getStatus());
        verify(ctx.prescriptionRepository, never()).save(dispensed);
    }

    @Test
    void escapesSpecialCharactersInPrescriptionAuditDetail() throws Exception {
        String reason = "Bệnh \"nhân\" \\ bỏ\nvề";
        TestContext ctx = context(true, mock(MedicalRecord.class));
        Prescription pending = pendingPrescription(UUID.randomUUID(), ctx.medicalRecordId, ctx.doctorId);
        when(ctx.prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(
                ctx.medicalRecordId, PrescriptionStatus.PENDING_DISPENSE)).thenReturn(List.of(pending));

        ctx.service.close(new CloseVisitCommand(ctx.item.getId(), VisitCloseOutcome.CANCELLED, reason));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(ctx.auditLogRepository).save(captor.capture());

        AuditLog log = captor.getValue();
        com.fasterxml.jackson.databind.JsonNode node = new ObjectMapper().readTree(log.getDetail());
        assertEquals(reason, node.get("cancelReason").asText());
        assertTrue(node.has("prescriptionCode"));
        assertTrue(node.has("cancelledAt"));
    }

    private Prescription pendingPrescription(UUID id, UUID medicalRecordId, UUID doctorId) {
        return Prescription.restore(id, "RX-" + id, medicalRecordId, PrescriptionStatus.PENDING_DISPENSE,
                "Note", doctorId, NOW, null, null,
                List.of(PrescriptionItem.restore(UUID.randomUUID(), id, UUID.randomUUID(), "Paracetamol",
                        "Paracetamol", "500 mg", "tablet", "1 tablet", 2, AdministrationRoute.ORAL, 2, 4, null,
                        NOW, null)));
    }

    private Prescription dispensedPrescription(UUID id, UUID medicalRecordId, UUID doctorId) {
        return Prescription.restore(id, "RX-" + id, medicalRecordId, PrescriptionStatus.DISPENSED,
                "Note", doctorId, NOW, null, null,
                List.of(PrescriptionItem.restore(UUID.randomUUID(), id, UUID.randomUUID(), "Paracetamol",
                        "Paracetamol", "500 mg", "tablet", "1 tablet", 2, AdministrationRoute.ORAL, 2, 4, null,
                        NOW, null)));
    }

    private TestContext context(boolean withAppointment, MedicalRecord medicalRecord) {
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = withAppointment ? UUID.randomUUID() : null;
        UUID actorId = UUID.randomUUID();
        UUID medicalRecordId = medicalRecord != null ? UUID.randomUUID() : null;

        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 9, 10), NOW);
        Visit visit = Visit.create("VIS900001", patientId, doctorId, appointmentId, null, VisitType.APPOINTMENT,
                NOW, "Consultation", null, actorId);
        visit.start(NOW.plusSeconds(30));
        QueueItem item = QueueItem.create(queue.getId(), patientId, appointmentId, visit.getId(),
                withAppointment ? QueueItemSourceType.APPOINTMENT : QueueItemSourceType.WALK_IN,
                1, LocalDate.of(2026, 9, 10), actorId, NOW);
        item.call(NOW.plusSeconds(30));

        Appointment appointment = withAppointment
                ? Appointment.restore(appointmentId, "AP900001", patientId, doctorId, NOW.plusSeconds(3600),
                        NOW.plusSeconds(5400), AppointmentStatus.IN_PROGRESS, "Consultation", null, NOW, null,
                        actorId, NOW)
                : null;

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        QueueAuditService auditService = mock(QueueAuditService.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        QueueItemQueryRepository queryRepository = mock(QueueItemQueryRepository.class);

        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findByIdForUpdate(queue.getId())).thenReturn(Optional.of(queue));
        when(visitRepository.findByIdForUpdate(visit.getId())).thenReturn(Optional.of(visit));
        if (appointment != null) {
            when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        }
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(clockPort.now()).thenReturn(NOW.plusSeconds(60));
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(queryRepository.findDetailById(item.getId())).thenReturn(Optional.of(new QueueItemResult(
                item.getId(), queue.getId(), patientId, "Nguyen Van A", doctorId, "Bac si Nguyen Van B", queue.getRoomId(), "P101",
                appointmentId, visit.getId(), "VIS900001", item.getSourceType(), QueueItemStatus.CANCELLED,
                item.getQueueNumber(), item.getQueueDate(), item.getCheckedInAt(), item.getCalledAt(), null,
                NOW.plusSeconds(60), "reason", null, null)));

        if (medicalRecord != null) {
            when(medicalRecord.getId()).thenReturn(medicalRecordId);
            when(medicalRecord.isContentLocked()).thenReturn(false);
            when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(medicalRecord));
            when(medicalRecordRepository.findByIdForUpdate(medicalRecordId)).thenReturn(Optional.of(medicalRecord));
            when(prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(medicalRecordId,
                    PrescriptionStatus.PENDING_DISPENSE)).thenReturn(List.of());
            when(prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(medicalRecordId,
                    PrescriptionStatus.DISPENSED)).thenReturn(List.of());
        } else {
            when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
        }

        CloseVisitService service = new CloseVisitService(queueItemRepository, medicalQueueRepository,
                visitRepository, appointmentRepository, medicalRecordRepository, prescriptionRepository,
                new QueueOperationAuthorization(currentUserPort), queryRepository, clockPort, auditService,
                auditLogRepository, currentUserPort, new com.fasterxml.jackson.databind.ObjectMapper());

        return new TestContext(service, queue, item, visit, appointment, medicalRecord, medicalRecordId,
                queueItemRepository, visitRepository, appointmentRepository, prescriptionRepository, currentUserPort,
                auditService, auditLogRepository, doctorId);
    }

    private record TestContext(
            CloseVisitService service,
            MedicalQueue queue,
            QueueItem item,
            Visit visit,
            Appointment appointment,
            MedicalRecord medicalRecord,
            UUID medicalRecordId,
            QueueItemRepository queueItemRepository,
            VisitRepository visitRepository,
            AppointmentRepository appointmentRepository,
            PrescriptionRepository prescriptionRepository,
            CurrentUserPort currentUserPort,
            QueueAuditService auditService,
            AuditLogRepository auditLogRepository,
            UUID doctorId) {
    }
}

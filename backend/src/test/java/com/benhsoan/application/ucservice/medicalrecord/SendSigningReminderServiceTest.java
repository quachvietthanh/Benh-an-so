package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordSigningReminder;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotOverdueException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.SendSigningReminderCommand;
import com.benhsoan.port.dto.result.SigningReminderResult;
import com.benhsoan.port.outbound.notification.NotificationSendResult;
import com.benhsoan.port.outbound.notification.SigningReminderMessage;
import com.benhsoan.port.outbound.notification.SigningReminderNotificationPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordSigningReminderRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendSigningReminderService - Unit Tests (NCL-11-CN-006)")
class SendSigningReminderServiceTest {

        @Mock
        private MedicalRecordRepository medicalRecordRepository;
        @Mock
        private VisitRepository visitRepository;
        @Mock
        private ClinicConfigurationRepository clinicConfigurationRepository;
        @Mock
        private MedicalRecordSigningReminderRepository reminderRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private PatientRepository patientRepository;
        @Mock
        private SigningReminderNotificationPort notificationPort;
        @Mock
        private AuditLogRepository auditLogRepository;
        @Mock
        private CurrentUserPort currentUserPort;
        @Mock
        private ClockPort clockPort;
        @Spy
        private ObjectMapper objectMapper = new ObjectMapper();

        @InjectMocks
        private SendSigningReminderService service;

        private final UUID recordId = UUID.randomUUID();
        private final UUID visitId = UUID.randomUUID();
        private final UUID doctorId = UUID.randomUUID();
        private final UUID patientId = UUID.randomUUID();
        private final UUID actorId = UUID.randomUUID();
        private final Instant completedAt = Instant.parse("2026-09-15T08:00:00Z");
        private final Instant now = Instant.parse("2026-09-16T12:00:00Z"); // 28 hours after completion

        private MedicalRecord createDraftRecord() {
                return MedicalRecord.restore(
                                recordId, visitId, "Headache", "Fever", null, null, null, null, null, null,
                                MedicalRecordStatus.DRAFT, null, null, null, null, null, actorId, completedAt, null,
                                null);
        }

        private Visit createCompletedVisit() {
                return Visit.restore(
                                visitId, "KB-001", patientId, doctorId, null, null,
                                VisitType.APPOINTMENT, VisitStatus.COMPLETED,
                                completedAt.minus(Duration.ofHours(1)), completedAt.minus(Duration.ofMinutes(50)),
                                completedAt,
                                "Headache", "Note", actorId, completedAt, completedAt);
        }

        @Test
        @DisplayName("Successfully send reminder when overdue and log audit")
        void sendReminderSuccess() {
                MedicalRecord record = createDraftRecord();
                Visit visit = createCompletedVisit();

                when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
                when(clinicConfigurationRepository.find()).thenReturn(Optional.empty()); // default 24h
                when(clockPort.now()).thenReturn(now);
                when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
                when(notificationPort.sendSigningReminder(any(SigningReminderMessage.class)))
                                .thenReturn(NotificationSendResult.delivered());

                when(reminderRepository.save(any(MedicalRecordSigningReminder.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                SigningReminderResult result = service
                                .sendReminder(new SendSigningReminderCommand(recordId, "SYSTEM", "Nhắc ký bệnh án"));

                assertNotNull(result);
                assertEquals(recordId, result.medicalRecordId());
                assertEquals(doctorId, result.doctorId());
                assertEquals(4, result.overdueHours()); // 28 - 24 = 4 hours
                assertEquals("SYSTEM", result.channel());
                assertEquals("Nhắc ký bệnh án", result.notes());
                assertEquals("SENT", result.status());

                verify(reminderRepository).save(any(MedicalRecordSigningReminder.class));
                verify(notificationPort).sendSigningReminder(any(SigningReminderMessage.class));
                verify(auditLogRepository).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Successfully records reminder with FAILED status when notification delivery fails")
        void sendReminderSuccess_NotificationFailed_SavedWithFailedStatus() {
                MedicalRecord record = createDraftRecord();
                Visit visit = createCompletedVisit();

                when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
                when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
                when(clockPort.now()).thenReturn(now);
                when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
                when(notificationPort.sendSigningReminder(any(SigningReminderMessage.class)))
                                .thenReturn(NotificationSendResult.failed("SMS gateway timeout"));

                when(reminderRepository.save(any(MedicalRecordSigningReminder.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                SigningReminderResult result = service
                                .sendReminder(new SendSigningReminderCommand(recordId, "SYSTEM", "Nhắc ký"));

                assertNotNull(result);
                assertEquals("FAILED", result.status());
                verify(reminderRepository).save(any(MedicalRecordSigningReminder.class));
                verify(auditLogRepository).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Successfully records reminder with FAILED status when notification port throws exception")
        void sendReminderSuccess_NotificationThrowsException_CaughtAndSavedWithFailedStatus() {
                MedicalRecord record = createDraftRecord();
                Visit visit = createCompletedVisit();

                when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
                when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
                when(clockPort.now()).thenReturn(now);
                when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
                when(notificationPort.sendSigningReminder(any(SigningReminderMessage.class)))
                                .thenThrow(new RuntimeException("Connection refused"));

                when(reminderRepository.save(any(MedicalRecordSigningReminder.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                SigningReminderResult result = service
                                .sendReminder(new SendSigningReminderCommand(recordId, "SYSTEM", "Nhắc ký"));

                assertNotNull(result);
                assertEquals("FAILED", result.status());
                verify(reminderRepository).save(any(MedicalRecordSigningReminder.class));
                verify(auditLogRepository).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Throws exception when record is already signed")
        void recordAlreadySignedThrows() {
                MedicalRecord record = MedicalRecord.restore(
                                recordId, visitId, "Headache", "Fever", null, null, null, null, null, null,
                                MedicalRecordStatus.SIGNED, "signature", completedAt, doctorId, null, null, actorId,
                                completedAt, null, null);

                when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

                assertThrows(MedicalRecordNotOverdueException.class,
                                () -> service.sendReminder(new SendSigningReminderCommand(recordId, null, null)));

                verifyNoInteractions(notificationPort);
                verifyNoInteractions(reminderRepository);
        }

        @Test
        @DisplayName("Throws exception when visit is not completed (IN_PROGRESS)")
        void visitNotCompletedThrows() {
                MedicalRecord record = createDraftRecord();
                Visit visit = Visit.restore(
                                visitId, "KB-002", patientId, doctorId, null, null,
                                VisitType.APPOINTMENT, VisitStatus.IN_PROGRESS,
                                completedAt, completedAt, null,
                                "Checkup", "Note", actorId, completedAt, completedAt);

                when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

                assertThrows(MedicalRecordNotOverdueException.class,
                                () -> service.sendReminder(new SendSigningReminderCommand(recordId, null, null)));

                verifyNoInteractions(notificationPort);
        }

        @Test
        @DisplayName("Throws exception when visit is EARLY_ENDED (closed without completion)")
        void earlyEndedVisitThrows() {
                MedicalRecord record = createDraftRecord();
                Visit visit = Visit.restore(
                                visitId, "KB-003", patientId, doctorId, null, null,
                                Specialty.GENERAL_ID,
                                VisitType.APPOINTMENT, VisitStatus.EARLY_ENDED,
                                completedAt.minus(Duration.ofHours(2)), completedAt.minus(Duration.ofHours(2)), null,
                                "Checkup", "Note", "Patient left", completedAt, actorId, completedAt, completedAt);

                when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

                assertThrows(MedicalRecordNotOverdueException.class,
                                () -> service.sendReminder(new SendSigningReminderCommand(recordId, null, null)));

                verifyNoInteractions(notificationPort);
        }

        @Test
        @DisplayName("Throws exception when record is not yet overdue")
        void notYetOverdueThrows() {
                MedicalRecord record = createDraftRecord();
                Visit visit = createCompletedVisit(); // completed at 08:00 on 2026-09-15
                Instant notOverdueNow = completedAt.plus(Duration.ofHours(10)); // only 10h passed, deadline is 24h

                when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
                when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
                when(clockPort.now()).thenReturn(notOverdueNow);

                assertThrows(MedicalRecordNotOverdueException.class,
                                () -> service.sendReminder(new SendSigningReminderCommand(recordId, null, null)));

                verifyNoInteractions(notificationPort);
        }
}

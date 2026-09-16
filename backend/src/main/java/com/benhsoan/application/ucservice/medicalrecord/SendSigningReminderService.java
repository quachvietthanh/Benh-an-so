package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordSigningReminder;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotOverdueException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.SendSigningReminderCommand;
import com.benhsoan.port.dto.result.SigningReminderResult;
import com.benhsoan.port.inbound.medicalrecord.SendSigningReminderUseCase;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SendSigningReminderService implements SendSigningReminderUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final MedicalRecordSigningReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final SigningReminderNotificationPort notificationPort;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public SigningReminderResult sendReminder(SendSigningReminderCommand command) {
        if (command == null || command.medicalRecordId() == null) {
            throw new ValidationException("Medical record ID is required.");
        }

        MedicalRecord record = medicalRecordRepository.findById(command.medicalRecordId())
                .orElseThrow(() -> new MedicalRecordNotFoundException(command.medicalRecordId()));

        if (record.isSigned()) {
            throw new MedicalRecordNotOverdueException("Bệnh án đã được ký số, không thể gửi nhắc.");
        }

        UUID visitId = record.getVisitId();
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));

        if (!visit.isCompleted() || visit.getCompletedAt() == null) {
            throw new MedicalRecordNotOverdueException("Lượt khám chưa hoàn thành, không thể gửi nhắc.");
        }

        Instant visitCompletedAt = visit.getCompletedAt();

        int signingDeadlineHours = clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getSigningDeadlineHours)
                .orElse(ClinicConfiguration.DEFAULT_SIGNING_DEADLINE_HOURS);

        Instant now = clockPort.now();
        Instant deadlineAt = visitCompletedAt.plus(Duration.ofHours(signingDeadlineHours));
        if (now.isBefore(deadlineAt)) {
            throw new MedicalRecordNotOverdueException("Bệnh án chưa quá thời hạn ký quy định (" + signingDeadlineHours + " giờ).");
        }

        long overdueHours = Math.max(0, Duration.between(deadlineAt, now).toHours());
        UUID doctorId = visit.getDoctorId();
        UUID actorId = currentUserPort.getCurrentUserId();

        User doctor = userRepository.findById(doctorId).orElse(null);
        String doctorName = doctor != null ? doctor.getFullName() : "Bác sĩ phụ trách";
        String doctorEmail = doctor != null ? doctor.getEmail() : null;
        String doctorPhone = doctor != null ? doctor.getPhone() : null;

        User actor = userRepository.findById(actorId).orElse(null);
        String actorName = actor != null ? actor.getFullName() : "Người dùng hệ thống";

        Patient patient = patientRepository.findById(visit.getPatientId()).orElse(null);
        String patientName = patient != null ? patient.getFullName() : "Bệnh nhân";

        String reminderStatus = "SENT";
        try {
            NotificationSendResult sendResult = notificationPort.sendSigningReminder(new SigningReminderMessage(
                    record.getId(),
                    visit.getVisitCode(),
                    doctorId,
                    doctorName,
                    doctorEmail,
                    doctorPhone,
                    patientName,
                    overdueHours,
                    deadlineAt,
                    command.notes()
            ));
            if (sendResult == null || !sendResult.sent()) {
                reminderStatus = "FAILED";
                log.warn("Signing reminder notification failed for record {}: {}", record.getId(),
                        sendResult != null ? sendResult.failureReason() : "null result");
            }
        } catch (Exception e) {
            reminderStatus = "FAILED";
            log.error("Exception occurred while sending signing reminder notification for record {}", record.getId(), e);
        }

        MedicalRecordSigningReminder reminder = MedicalRecordSigningReminder.create(
                record.getId(),
                doctorId,
                actorId,
                now,
                overdueHours,
                command.channel(),
                command.notes(),
                reminderStatus
        );
        MedicalRecordSigningReminder savedReminder = reminderRepository.save(reminder);

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.SEND,
                ResourceType.MEDICAL_RECORD,
                record.getId(),
                buildAuditDetail(record.getId(), visit.getVisitCode(), doctorId, doctorName, overdueHours, now, reminderStatus),
                null,
                now
        ));

        return new SigningReminderResult(
                savedReminder.getId(),
                record.getId(),
                doctorId,
                doctorName,
                actorId,
                actorName,
                savedReminder.getRemindedAt(),
                savedReminder.getOverdueHours(),
                savedReminder.getChannel(),
                savedReminder.getNotes(),
                savedReminder.getStatus()
        );
    }

    private String buildAuditDetail(
            UUID medicalRecordId,
            String visitCode,
            UUID doctorId,
            String doctorName,
            long overdueHours,
            Instant remindedAt,
            String deliveryStatus
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", "SIGNING_REMINDER");
        detail.put("medicalRecordId", medicalRecordId.toString());
        detail.put("visitCode", visitCode);
        detail.put("doctorId", doctorId.toString());
        detail.put("doctorName", doctorName);
        detail.put("overdueHours", overdueHours);
        detail.put("remindedAt", remindedAt.toString());
        detail.put("deliveryStatus", deliveryStatus);
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            return "{\"action\":\"SIGNING_REMINDER\",\"medicalRecordId\":\"" + medicalRecordId + "\"}";
        }
    }
}

package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.config.AppointmentReminderProperties;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.notification.AppointmentNotificationLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.outbound.notification.AppointmentNotificationPort;
import com.benhsoan.port.outbound.notification.AppointmentReminderMessage;
import com.benhsoan.port.outbound.notification.NotificationSendResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentNotificationLogRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.dto.result.AppointmentReminderResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessAppointmentReminderService {

    private static final DateTimeFormatter REMINDER_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm 'ngay' dd/MM/yyyy");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentNotificationLogRepository notificationLogRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentNotificationPort appointmentNotificationPort;
    private final AppointmentReminderProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppointmentReminderResult process(UUID appointmentId, Instant now) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElse(null);
        if (appointment == null) {
            return AppointmentReminderResult.skipped("Appointment no longer exists.");
        }
        if (!isStillEligible(appointment, now)) {
            return AppointmentReminderResult.skipped(
                    "Appointment is not eligible for a reminder.");
        }
        if (notificationLogRepository.existsSentReminderByAppointmentId(appointmentId)) {
            return AppointmentReminderResult.skipped("Reminder was already sent.");
        }

        Patient patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
        User doctor = userRepository.findById(appointment.getDoctorId()).orElse(null);
        if (patient == null || doctor == null) {
            saveFailedLog(appointment, "Missing patient or doctor information", now);
            return AppointmentReminderResult.failed("Missing patient or doctor information.");
        }

        String content = buildContent(appointment, patient, doctor);
        AppointmentReminderMessage message = new AppointmentReminderMessage(
                appointment.getId(), appointment.getAppointmentCode(), patient.getFullName(),
                doctor.getFullName(), appointment.getStartTime(), content);

        try {
            NotificationSendResult result = appointmentNotificationPort.sendAppointmentReminder(message);
            if (result != null && result.sent()) {
                notificationLogRepository.save(AppointmentNotificationLog.sent(
                        appointment.getId(), appointment.getPatientId(), content, now));
                return AppointmentReminderResult.sent();
            }

            String failureReason = failureReason(result);
            saveFailedLog(appointment, failureReason, now);
            return AppointmentReminderResult.failed(failureReason);
        } catch (RuntimeException exception) {
            log.error("Appointment reminder adapter failed for appointmentId={}", appointmentId,
                    exception);
            saveFailedLog(appointment, exception.getClass().getSimpleName(), now);
            return AppointmentReminderResult.failed("Notification adapter failed.");
        }
    }

    private boolean isStillEligible(Appointment appointment, Instant now) {
        return (appointment.getStatus() == AppointmentStatus.SCHEDULED
                || appointment.getStatus() == AppointmentStatus.CONFIRMED)
                && appointment.getStartTime().isAfter(now);
    }

    private String buildContent(Appointment appointment, Patient patient, User doctor) {
        ZoneId zoneId = properties.zoneId();
        String appointmentTime = REMINDER_TIME_FORMATTER
                .withZone(zoneId)
                .format(appointment.getStartTime());
        return "Kinh gui " + patient.getFullName() + ",\n\n"
                + "Quy khach co lich kham luc " + appointmentTime + ".\n"
                + "Ma lich hen: " + appointment.getAppointmentCode() + ".\n"
                + "Bac si: " + doctor.getFullName() + ".\n\n"
                + "Vui long den truoc 15 phut.\n\nXin cam on.";
    }

    private void saveFailedLog(Appointment appointment, String failureReason, Instant now) {
        notificationLogRepository.save(AppointmentNotificationLog.failed(
                appointment.getId(), appointment.getPatientId(),
                "Appointment reminder could not be delivered.", failureReason, now));
    }

    private String failureReason(NotificationSendResult result) {
        if (result == null) {
            return "Notification adapter returned no result";
        }
        if (result.failureReason() == null || result.failureReason().isBlank()) {
            return "Notification adapter reported failure";
        }
        return result.failureReason().substring(0,
                Math.min(result.failureReason().length(), 500));
    }
}

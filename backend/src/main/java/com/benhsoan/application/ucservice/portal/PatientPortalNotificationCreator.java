package com.benhsoan.application.ucservice.portal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentRescheduleLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NCL-14-CN-008 CV-03: creates the three patient-portal notification kinds at the
 * existing business trigger points. Notifications are only written for patients
 * that actually have a patient-portal account ({@code Patient.userId} linked), and
 * each kind is guarded against duplicate creation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientPortalNotificationCreator {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy").withZone(CLINIC_ZONE);

    private static final String REMINDER_TITLE = "Nhắc lịch hẹn";
    private static final String CHANGED_TITLE = "Lịch hẹn đã thay đổi";
    private static final String LAB_RESULT_TITLE = "Kết quả cận lâm sàng mới";
    private static final String LAB_RESULT_MESSAGE = "Bạn có kết quả cận lâm sàng mới.";

    private final PatientPortalNotificationRepository notificationRepository;
    private final PatientRepository patientRepository;

    public void createAppointmentReminder(Appointment appointment, Patient patient, User doctor, Instant now) {
        if (patient == null || patient.getUserId() == null) {
            return;
        }
        UUID patientId = appointment.getPatientId();
        if (notificationRepository.existsByPatientIdAndTypeAndAppointmentId(
                patientId, PatientPortalNotificationType.APPOINTMENT_REMINDER, appointment.getId())) {
            return;
        }
        String message = "Bạn có lịch hẹn lúc " + TIME_FORMATTER.format(appointment.getStartTime())
                + ". Mã lịch hẹn: " + appointment.getAppointmentCode()
                + ". Bác sĩ: " + doctor.getFullName() + ".";
        notificationRepository.save(PatientPortalNotification.reminder(
                patientId, REMINDER_TITLE, message, appointment.getId(), now));
    }

    public void createAppointmentChanged(Appointment appointment, AppointmentRescheduleLog log, Instant now) {
        Optional<Patient> patient = patientRepository.findById(appointment.getPatientId());
        if (patient.isEmpty() || patient.get().getUserId() == null) {
            return;
        }
        UUID patientId = appointment.getPatientId();
        if (notificationRepository.existsByPatientIdAndTypeAndRescheduleLogId(
                patientId, PatientPortalNotificationType.APPOINTMENT_CHANGED, log.getId())) {
            return;
        }
        String message = "Lịch hẹn " + appointment.getAppointmentCode()
                + " đã được dời từ " + TIME_FORMATTER.format(log.getOldStartTime())
                + " sang " + TIME_FORMATTER.format(log.getNewStartTime()) + ".";
        notificationRepository.save(PatientPortalNotification.changed(
                patientId, CHANGED_TITLE, message, appointment.getId(), log.getId(), now));
    }

    public void createLabResultAvailable(UUID patientId, UUID clinicalResultId, Instant now) {
        Optional<Patient> patient = patientRepository.findById(patientId);
        if (patient.isEmpty() || patient.get().getUserId() == null) {
            return;
        }
        if (notificationRepository.existsByPatientIdAndTypeAndClinicalResultId(
                patientId, PatientPortalNotificationType.LAB_RESULT_AVAILABLE, clinicalResultId)) {
            return;
        }
        notificationRepository.save(PatientPortalNotification.labResultAvailable(
                patientId, LAB_RESULT_TITLE, LAB_RESULT_MESSAGE, clinicalResultId, now));
    }
}

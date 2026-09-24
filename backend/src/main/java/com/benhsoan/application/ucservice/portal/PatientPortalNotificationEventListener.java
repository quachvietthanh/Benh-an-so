package com.benhsoan.application.ucservice.portal;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.benhsoan.domain.portal.notification.AppointmentChangedNotificationRequested;
import com.benhsoan.domain.portal.notification.AppointmentReminderNotificationRequested;
import com.benhsoan.domain.portal.notification.LabResultAvailableNotificationRequested;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NCL-14-CN-008: creates patient-portal notifications only after the core
 * clinical/appointment transaction has committed (AFTER_COMMIT). Each handler is
 * deliberately fault-isolated: only persistence failures ({@link DataAccessException})
 * are swallowed and logged, so a notification outage can never roll back or fail
 * the core operation, while genuine programming errors still propagate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientPortalNotificationEventListener {

    private final PatientPortalNotificationCreator creator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentReminderRequested(AppointmentReminderNotificationRequested event) {
        try {
            creator.createAppointmentReminder(
                    event.appointment(), event.patient(), event.doctor(), event.now());
        } catch (DataAccessException exception) {
            log.warn("Failed to persist appointment reminder portal notification for appointmentId={}",
                    event.appointment().getId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentChangedRequested(AppointmentChangedNotificationRequested event) {
        try {
            creator.createAppointmentChanged(event.appointment(), event.rescheduleLog(), event.now());
        } catch (DataAccessException exception) {
            log.warn("Failed to persist appointment changed portal notification for rescheduleLogId={}",
                    event.rescheduleLog().getId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLabResultAvailableRequested(LabResultAvailableNotificationRequested event) {
        try {
            creator.createLabResultAvailable(event.patientId(), event.clinicalResultId(), event.now());
        } catch (DataAccessException exception) {
            log.warn("Failed to persist lab result portal notification for clinicalResultId={}",
                    event.clinicalResultId(), exception);
        }
    }
}

package com.benhsoan.application.ucservice.followup;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.port.dto.result.FollowUpReminderResult;

@Component
public class FollowUpReminderResultMapper {

    public FollowUpReminderResult toResult(FollowUpReminder reminder) {
        if (reminder == null) {
            return null;
        }
        return new FollowUpReminderResult(
                reminder.getId(),
                reminder.getPatientId(),
                reminder.getVisitId(),
                reminder.getAppointmentId(),
                reminder.getFollowUpDate(),
                reminder.getRemindAt(),
                reminder.getReminderType(),
                reminder.getStatus(),
                reminder.getNotes(),
                reminder.getCreatedBy(),
                reminder.getCreatedAt()
        );
    }
}

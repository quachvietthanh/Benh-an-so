package com.benhsoan.domain.portal.notification;

/**
 * NCL-14-CN-008: the three patient-portal notification scenarios explicitly
 * supported by the workbook ("nhắc lịch trước giờ hẹn", "lịch bị đổi", "có kết
 * quả cận lâm sàng mới"). No other channel/category is introduced.
 *
 * NCL-14-CN-010 TC-03 adds exactly one more category: the guardian-link review
 * reminder raised when a dependent reaches adulthood while a guardian link still
 * exists. The affected accounts are notified so the link can be reviewed; the
 * relationship itself is never removed automatically.
 */
public enum PatientPortalNotificationType {

    APPOINTMENT_REMINDER,

    APPOINTMENT_CHANGED,

    LAB_RESULT_AVAILABLE,

    GUARDIAN_LINK_REVIEW
}

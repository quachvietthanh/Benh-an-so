package com.benhsoan.domain.portal.notification;

/**
 * NCL-14-CN-008: the three patient-portal notification scenarios explicitly
 * supported by the workbook ("nhắc lịch trước giờ hẹn", "lịch bị đổi", "có kết
 * quả cận lâm sàng mới"). No other channel/category is introduced.
 */
public enum PatientPortalNotificationType {

    APPOINTMENT_REMINDER,

    APPOINTMENT_CHANGED,

    LAB_RESULT_AVAILABLE
}

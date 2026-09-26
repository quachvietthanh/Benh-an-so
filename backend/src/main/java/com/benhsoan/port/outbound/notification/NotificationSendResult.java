package com.benhsoan.port.outbound.notification;

public record NotificationSendResult(
        boolean sent,
        String failureReason
) {
    public static NotificationSendResult delivered() {
        return new NotificationSendResult(true, null);
    }

    public static NotificationSendResult failed(String failureReason) {
        return new NotificationSendResult(false, failureReason);
    }
}

package com.benhsoan.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.domain.appointment.exception.WaitlistInvalidStatusException;
import com.benhsoan.domain.shared.exception.ValidationException;

class AppointmentWaitlistTest {

    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID createdBy = UUID.randomUUID();
    private final LocalDate desiredDate = LocalDate.of(2026, 10, 15);
    private final Instant now = Instant.parse("2026-10-01T08:00:00Z");

    @Test
    @DisplayName("Tạo mới mục danh sách chờ với trạng thái WAITING và ca khám ANYTIME mặc định")
    void shouldCreateWaitlistSuccessfully() {
        AppointmentWaitlist waitlist = AppointmentWaitlist.create(
                patientId,
                doctorId,
                desiredDate,
                null,
                "Bệnh nhân muốn khám đầu giờ",
                createdBy,
                now
        );

        assertThat(waitlist.getId()).isNotNull();
        assertThat(waitlist.getPatientId()).isEqualTo(patientId);
        assertThat(waitlist.getDoctorId()).isEqualTo(doctorId);
        assertThat(waitlist.getDesiredDate()).isEqualTo(desiredDate);
        assertThat(waitlist.getTimePreference()).isEqualTo(TimePreference.ANYTIME);
        assertThat(waitlist.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(waitlist.getNote()).isEqualTo("Bệnh nhân muốn khám đầu giờ");
        assertThat(waitlist.getCreatedBy()).isEqualTo(createdBy);
        assertThat(waitlist.getCreatedAt()).isEqualTo(now);
        assertThat(waitlist.getBookedAppointmentId()).isNull();
        assertThat(waitlist.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("Ném NullPointerException khi thiếu trường bắt buộc")
    void shouldThrowExceptionWhenRequiredFieldMissing() {
        assertThatThrownBy(() -> AppointmentWaitlist.create(
                null, doctorId, desiredDate, TimePreference.MORNING, null, createdBy, now))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID bệnh nhân");

        assertThatThrownBy(() -> AppointmentWaitlist.create(
                patientId, null, desiredDate, TimePreference.MORNING, null, createdBy, now))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID bác sĩ");

        assertThatThrownBy(() -> AppointmentWaitlist.create(
                patientId, doctorId, null, TimePreference.MORNING, null, createdBy, now))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Ngày mong muốn khám");

        assertThatThrownBy(() -> AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.MORNING, null, null, now))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Người tạo");
    }

    @Test
    @DisplayName("markScheduled chuyển trạng thái thành SCHEDULED và lưu bookedAppointmentId")
    void shouldMarkScheduledSuccessfully() {
        AppointmentWaitlist waitlist = AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.MORNING, null, createdBy, now);

        UUID appointmentId = UUID.randomUUID();
        Instant scheduledAt = Instant.parse("2026-10-02T10:00:00Z");
        waitlist.markScheduled(appointmentId, scheduledAt);

        assertThat(waitlist.getStatus()).isEqualTo(WaitlistStatus.SCHEDULED);
        assertThat(waitlist.getBookedAppointmentId()).isEqualTo(appointmentId);
        assertThat(waitlist.getUpdatedAt()).isEqualTo(scheduledAt);
    }

    @Test
    @DisplayName("markScheduled ném ngoại lệ khi trạng thái không phải WAITING")
    void shouldThrowWhenMarkScheduledOnNonWaitingStatus() {
        AppointmentWaitlist waitlist = AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.MORNING, null, createdBy, now);
        waitlist.cancel("Bệnh nhân không muốn chờ", now);

        assertThatThrownBy(() -> waitlist.markScheduled(UUID.randomUUID(), now))
                .isInstanceOf(WaitlistInvalidStatusException.class);
    }

    @Test
    @DisplayName("cancel chuyển trạng thái thành CANCELLED và lưu lý do hủy")
    void shouldCancelSuccessfully() {
        AppointmentWaitlist waitlist = AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.AFTERNOON, null, createdBy, now);

        Instant cancelledAt = Instant.parse("2026-10-03T09:00:00Z");
        waitlist.cancel("Bệnh nhân chuyển đi công tác", cancelledAt);

        assertThat(waitlist.getStatus()).isEqualTo(WaitlistStatus.CANCELLED);
        assertThat(waitlist.getCancelReason()).isEqualTo("Bệnh nhân chuyển đi công tác");
        assertThat(waitlist.getUpdatedAt()).isEqualTo(cancelledAt);
    }

    @Test
    @DisplayName("cancel ném ngoại lệ khi lý do hủy trống")
    void shouldThrowWhenCancelReasonIsBlank() {
        AppointmentWaitlist waitlist = AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.AFTERNOON, null, createdBy, now);

        assertThatThrownBy(() -> waitlist.cancel("   ", now))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("markExpired chuyển trạng thái thành EXPIRED khi đang WAITING")
    void shouldMarkExpiredSuccessfully() {
        AppointmentWaitlist waitlist = AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.ANYTIME, null, createdBy, now);

        Instant expiredAt = Instant.parse("2026-10-16T00:00:00Z");
        waitlist.markExpired(expiredAt);

        assertThat(waitlist.getStatus()).isEqualTo(WaitlistStatus.EXPIRED);
        assertThat(waitlist.getUpdatedAt()).isEqualTo(expiredAt);
    }
}

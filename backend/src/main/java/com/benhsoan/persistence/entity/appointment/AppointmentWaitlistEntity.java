package com.benhsoan.persistence.entity.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointment_waitlist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentWaitlistEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID doctorId;

    @Column(name = "desired_date", nullable = false)
    private LocalDate desiredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_preference", nullable = false, length = 30)
    private TimePreference timePreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WaitlistStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "booked_appointment_id", columnDefinition = "BINARY(16)")
    private UUID bookedAppointmentId;

    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

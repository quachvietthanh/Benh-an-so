package com.benhsoan.persistence.entity.appointment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointment_reschedule_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRescheduleLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "appointment_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID appointmentId;

    @Column(name = "old_doctor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID oldDoctorId;

    @Column(name = "new_doctor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID newDoctorId;

    @Column(name = "old_start_time", nullable = false)
    private Instant oldStartTime;

    @Column(name = "old_end_time", nullable = false)
    private Instant oldEndTime;

    @Column(name = "new_start_time", nullable = false)
    private Instant newStartTime;

    @Column(name = "new_end_time", nullable = false)
    private Instant newEndTime;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "rescheduled_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID rescheduledBy;

    @Column(name = "rescheduled_at", nullable = false)
    private Instant rescheduledAt;

}

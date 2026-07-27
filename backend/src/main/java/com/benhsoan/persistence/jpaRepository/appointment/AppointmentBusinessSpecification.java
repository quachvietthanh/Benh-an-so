package com.benhsoan.persistence.jpaRepository.appointment;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;

public final class AppointmentBusinessSpecification {

    private AppointmentBusinessSpecification() {
    }

    public static Specification<AppointmentEntity> hasDoctor(
            UUID doctorId
    ) {
        return (root, query, cb)
                -> cb.equal(root.get("doctorId"), doctorId);
    }

    public static Specification<AppointmentEntity> hasPatient(
            UUID patientId
    ) {
        return (root, query, cb)
                -> cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<AppointmentEntity> hasAppointmentCode(
            String appointmentCode
    ) {
        return (root, query, cb)
                -> cb.equal(root.get("appointmentCode"), appointmentCode);
    }

    public static Specification<AppointmentEntity> hasStatus(
            AppointmentStatus status
    ) {
        return (root, query, cb)
                -> cb.equal(root.get("status"), status);
    }

    public static Specification<AppointmentEntity> active() {
        return (root, query, cb)
                -> root.get("status").in(
                        AppointmentStatus.SCHEDULED,
                        AppointmentStatus.CHECKED_IN,
                        AppointmentStatus.IN_PROGRESS
                );
    }

    public static Specification<AppointmentEntity> overlap(
            Instant startTime,
            Instant endTime
    ) {
        return (root, query, cb)
                -> cb.and(
                        cb.lessThan(root.get("startTime"), endTime),
                        cb.greaterThan(root.get("endTime"), startTime)
                );
    }

    public static Specification<AppointmentEntity> createdBy(
            UUID createdBy
    ) {
        return (root, query, cb)
                -> cb.equal(root.get("createdBy"), createdBy);
    }

    public static Specification<AppointmentEntity> today(
            Instant startOfDay,
            Instant endOfDay
    ) {
        return (root, query, cb)
                -> cb.between(
                        root.get("startTime"),
                        startOfDay,
                        endOfDay
                );
    }

    public static Specification<AppointmentEntity> overdue(
            Instant threshold
    ) {
        return (root, query, cb)
                -> cb.and(
                        cb.equal(
                                root.get("status"),
                                AppointmentStatus.SCHEDULED
                        ),
                        cb.lessThan(
                                root.get("startTime"),
                                threshold
                        )
                );
    }
}

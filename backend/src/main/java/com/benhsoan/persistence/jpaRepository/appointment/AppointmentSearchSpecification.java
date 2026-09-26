package com.benhsoan.persistence.jpaRepository.appointment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;

public final class AppointmentSearchSpecification {

    private AppointmentSearchSpecification() {
    }

    public static Specification<AppointmentEntity> build(
            SearchAppointmentCommand command
    ) {
        List<Specification<AppointmentEntity>> predicates = new ArrayList<>();

        if (command.patientId() != null) {
            predicates.add(hasPatient(command.patientId()));
        }

        if (command.doctorId() != null) {
            predicates.add(hasDoctor(command.doctorId()));
        }

        if (command.status() != null) {
            predicates.add(hasStatus(command.status()));
        }

        if (command.startDate() != null) {
            predicates.add(startAfter(command.startDate()));
        }

        if (command.endDate() != null) {
            predicates.add(endBefore(command.endDate()));
        }

        if (predicates.isEmpty()) {
            return Specification.allOf();
        }
        return Specification.allOf(predicates);
    }

    public static Specification<AppointmentEntity> hasPatient(
            UUID patientId
    ) {
        return (root, query, cb) ->
                cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<AppointmentEntity> hasDoctor(
            UUID doctorId
    ) {
        return (root, query, cb) ->
                cb.equal(root.get("doctorId"), doctorId);
    }

    public static Specification<AppointmentEntity> hasStatus(
            AppointmentStatus status
    ) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<AppointmentEntity> startAfter(
            Instant start
    ) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("startTime"), start);
    }

    public static Specification<AppointmentEntity> endBefore(
            Instant end
    ) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("endTime"), end);
    }

}

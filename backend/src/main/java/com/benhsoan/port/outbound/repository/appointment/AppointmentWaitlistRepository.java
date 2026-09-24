package com.benhsoan.port.outbound.repository.appointment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.appointment.AppointmentWaitlist;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;

public interface AppointmentWaitlistRepository {

    AppointmentWaitlist save(AppointmentWaitlist waitlist);

    Optional<AppointmentWaitlist> findById(UUID id);

    Optional<AppointmentWaitlist> findByIdForUpdate(UUID id);

    Optional<AppointmentWaitlist> findFirstWaitingByDoctorAndDate(UUID doctorId, LocalDate desiredDate);

    List<AppointmentWaitlist> findWaitlist(UUID doctorId, LocalDate desiredDate, LocalDate fromDate, WaitlistStatus status);

    default List<AppointmentWaitlist> findWaitlist(UUID doctorId, LocalDate desiredDate, WaitlistStatus status) {
        return findWaitlist(doctorId, desiredDate, null, status);
    }


    boolean existsByPatientIdAndDoctorIdAndDesiredDateAndStatus(
            UUID patientId,
            UUID doctorId,
            LocalDate desiredDate,
            WaitlistStatus status
    );

    Optional<AppointmentWaitlist> findActiveByPatientAndDoctorAndDate(
            UUID patientId,
            UUID doctorId,
            LocalDate desiredDate
    );
}

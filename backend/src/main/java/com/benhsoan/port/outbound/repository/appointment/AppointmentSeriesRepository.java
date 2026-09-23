package com.benhsoan.port.outbound.repository.appointment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.appointment.AppointmentSeries;

public interface AppointmentSeriesRepository {

    Optional<AppointmentSeries> findById(UUID id);

    Optional<AppointmentSeries> findBySeriesCode(String seriesCode);

    List<AppointmentSeries> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    AppointmentSeries save(AppointmentSeries series);

    Optional<String> findHighestSeriesCode();
}

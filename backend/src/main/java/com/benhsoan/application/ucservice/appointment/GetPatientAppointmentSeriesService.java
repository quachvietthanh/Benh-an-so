package com.benhsoan.application.ucservice.appointment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentSeries;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult;
import com.benhsoan.port.inbound.appointment.GetPatientAppointmentSeriesUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentSeriesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientAppointmentSeriesService implements GetPatientAppointmentSeriesUseCase {

    private final AppointmentSeriesRepository appointmentSeriesRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesResultMapper appointmentSeriesResultMapper;
    private final AppointmentResultMapper appointmentResultMapper;

    @Override
    public List<AppointmentSeriesResult> getByPatientId(UUID patientId) {
        List<AppointmentSeries> seriesList = appointmentSeriesRepository.findByPatientIdOrderByCreatedAtDesc(patientId);

        return seriesList.stream()
                .map(series -> {
                    List<Appointment> appointments = appointmentRepository.findBySeriesIdOrderBySequenceNumberAsc(series.getId());
                    List<AppointmentResult> results = appointments.stream()
                            .map(appointmentResultMapper::toResult)
                            .toList();
                    return appointmentSeriesResultMapper.toResult(series, results);
                })
                .toList();
    }
}

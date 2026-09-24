package com.benhsoan.application.ucservice.appointment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentSeries;
import com.benhsoan.domain.appointment.exception.AppointmentSeriesNotFoundException;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult;
import com.benhsoan.port.inbound.appointment.GetAppointmentSeriesByIdUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentSeriesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAppointmentSeriesByIdService implements GetAppointmentSeriesByIdUseCase {

    private final AppointmentSeriesRepository appointmentSeriesRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesResultMapper appointmentSeriesResultMapper;
    private final AppointmentResultMapper appointmentResultMapper;

    @Override
    public AppointmentSeriesResult getById(UUID id) {
        AppointmentSeries series = appointmentSeriesRepository.findById(id)
                .orElseThrow(() -> new AppointmentSeriesNotFoundException(id));

        List<Appointment> appointments = appointmentRepository.findBySeriesIdOrderBySequenceNumberAsc(id);
        List<AppointmentResult> appointmentResults = appointments.stream()
                .map(appointmentResultMapper::toResult)
                .toList();

        return appointmentSeriesResultMapper.toResult(series, appointmentResults);
    }
}

package com.benhsoan.application.ucservice.appointment;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.GetAppointmentByIdUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAppointmentByIdService implements GetAppointmentByIdUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentResultMapper appointmentResultMapper;

    @Override
    public AppointmentResult getById(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(appointmentResultMapper::toResult)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
    }
}

package com.benhsoan.application.ucservice.appointment;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.SearchAppointmentsUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchAppointmentsService implements SearchAppointmentsUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentResultMapper appointmentResultMapper;

    @Override
    public Page<AppointmentResult> search(SearchAppointmentCommand command) {
        return appointmentRepository.search(command).map(appointmentResultMapper::toResult);
    }
}

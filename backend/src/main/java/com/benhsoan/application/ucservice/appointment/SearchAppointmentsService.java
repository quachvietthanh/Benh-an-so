package com.benhsoan.application.ucservice.appointment;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.SearchAppointmentsUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

@Service
@Transactional(readOnly = true)
public class SearchAppointmentsService implements SearchAppointmentsUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentResultMapper appointmentResultMapper;
    private final AppointmentResultAssembler assembler;

    public SearchAppointmentsService(
            AppointmentRepository appointmentRepository,
            AppointmentResultMapper appointmentResultMapper,
            AppointmentResultAssembler assembler
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentResultMapper = appointmentResultMapper;
        this.assembler = assembler;
    }

    public SearchAppointmentsService(
            AppointmentRepository appointmentRepository,
            AppointmentResultMapper appointmentResultMapper
    ) {
        this(appointmentRepository, appointmentResultMapper, null);
    }

    @Override
    public Page<AppointmentResult> search(SearchAppointmentCommand command) {
        Page<Appointment> page = appointmentRepository.search(command);
        return assembler != null
                ? assembler.toResultPage(page)
                : page.map(appointmentResultMapper::toResult);
    }
}

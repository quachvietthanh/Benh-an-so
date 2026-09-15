package com.benhsoan.application.ucservice.appointment;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.port.dto.command.appointment.GetOverdueAppointmentsCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.GetOverdueAppointmentsUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

@Service
@Transactional(readOnly = true)
public class GetOverdueAppointmentsService
        implements GetOverdueAppointmentsUseCase {

    private static final Duration NO_SHOW_THRESHOLD = Duration.ofMinutes(15);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentResultMapper resultMapper;
    private final AppointmentResultAssembler assembler;

    public GetOverdueAppointmentsService(
            AppointmentRepository appointmentRepository,
            AppointmentResultMapper resultMapper,
            AppointmentResultAssembler assembler
    ) {
        this.appointmentRepository = appointmentRepository;
        this.resultMapper = resultMapper;
        this.assembler = assembler;
    }

    public GetOverdueAppointmentsService(
            AppointmentRepository appointmentRepository,
            AppointmentResultMapper resultMapper
    ) {
        this(appointmentRepository, resultMapper, null);
    }

    @Override
    public Page<AppointmentResult> execute(
            GetOverdueAppointmentsCommand command
    ) {

        Instant threshold
                = Instant.now().minus(NO_SHOW_THRESHOLD);

        Page<Appointment> page = appointmentRepository.findOverdue(
                threshold,
                command.pageable()
        );

        return assembler != null
                ? assembler.toResultPage(page)
                : page.map(resultMapper::toResult);
    }

}

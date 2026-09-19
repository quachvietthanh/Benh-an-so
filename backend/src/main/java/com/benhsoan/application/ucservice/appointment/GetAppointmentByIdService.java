package com.benhsoan.application.ucservice.appointment;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.GetAppointmentByIdUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@Service
@Transactional(readOnly = true)
public class GetAppointmentByIdService implements GetAppointmentByIdUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentResultMapper appointmentResultMapper;
    private final AppointmentRescheduleHistoryAssembler historyAssembler;
    private final UserRepository userRepository;
    private final AppointmentResultAssembler appointmentResultAssembler;

    public GetAppointmentByIdService(
            AppointmentRepository appointmentRepository,
            AppointmentResultMapper appointmentResultMapper,
            AppointmentRescheduleHistoryAssembler historyAssembler,
            UserRepository userRepository,
            AppointmentResultAssembler appointmentResultAssembler
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentResultMapper = appointmentResultMapper;
        this.historyAssembler = historyAssembler;
        this.userRepository = userRepository;
        this.appointmentResultAssembler = appointmentResultAssembler;
    }

    public GetAppointmentByIdService(
            AppointmentRepository appointmentRepository,
            AppointmentResultMapper appointmentResultMapper,
            AppointmentRescheduleHistoryAssembler historyAssembler,
            UserRepository userRepository
    ) {
        this(appointmentRepository, appointmentResultMapper, historyAssembler, userRepository, null);
    }

    @Override
    public AppointmentResult getById(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(appointment -> {
                    if (appointmentResultAssembler != null) {
                        return appointmentResultAssembler.toResult(appointment);
                    }
                    var histories = historyAssembler.getHistoriesForAppointment(appointmentId);
                    String confirmedByName = null;
                    if (appointment.getConfirmedBy() != null) {
                        confirmedByName = userRepository.findById(appointment.getConfirmedBy())
                                .map(User::getFullName)
                                .orElse("Unknown");
                    }
                    return appointmentResultMapper.toResult(appointment, histories, confirmedByName);
                })
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
    }
}

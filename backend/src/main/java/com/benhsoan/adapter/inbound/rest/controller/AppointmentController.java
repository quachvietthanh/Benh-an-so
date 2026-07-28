package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.AppointmentRestMapper;
import com.benhsoan.adapter.inbound.rest.request.appointment.CancelAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.CreateAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentResponse;
import com.benhsoan.port.dto.command.appointment.GetOverdueAppointmentsCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.CancelAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.CreateAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.GetOverdueAppointmentsUseCase;
import com.benhsoan.port.inbound.appointment.MarkAppointmentNoShowUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Validated
public class AppointmentController {

    private final CreateAppointmentUseCase createAppointmentUseCase;

    private final CancelAppointmentUseCase cancelAppointmentUseCase;

    private final MarkAppointmentNoShowUseCase markAppointmentNoShowUseCase;

    private final GetOverdueAppointmentsUseCase getOverdueAppointmentsUseCase;

    private final AppointmentRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResult result
                = createAppointmentUseCase.create(
                        mapper.toCommand(request)
                );
        return mapper.toResponse(result);

    }

    @PatchMapping("/cancel/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public AppointmentResponse cancel(
            @PathVariable UUID id,
            @Valid
            @RequestBody CancelAppointmentRequest request) {

        AppointmentResult result
                = cancelAppointmentUseCase.cancel(
                        id,
                        mapper.toCommand(request)
                );
        return mapper.toResponse(result);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public Page<AppointmentResponse> getOverdueAppointments(Pageable pageable) {
        Page<AppointmentResult> result
                = getOverdueAppointmentsUseCase.execute(
                        GetOverdueAppointmentsCommand.builder()
                                .pageable(pageable)
                                .build()
                );

        return mapper.toResponse(result);
    }

    @PatchMapping("/no-show/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public AppointmentResponse markNoShow(@PathVariable UUID id) {
        AppointmentResult result
                = markAppointmentNoShowUseCase.execute(
                        mapper.toCommand(id)
                );

        return mapper.toResponse(result);
    }

}

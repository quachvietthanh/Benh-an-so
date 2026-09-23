package com.benhsoan.adapter.inbound.rest.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.AppointmentWaitlistRestMapper;
import com.benhsoan.adapter.inbound.rest.request.appointment.AddToWaitlistRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.CancelWaitlistRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentWaitlistResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.WaitlistSuggestionResponse;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.query.appointment.GetAppointmentWaitlistQuery;
import com.benhsoan.port.dto.query.appointment.GetWaitlistSuggestionQuery;
import com.benhsoan.port.inbound.appointment.AddToWaitlistUseCase;
import com.benhsoan.port.inbound.appointment.CancelWaitlistEntryUseCase;
import com.benhsoan.port.inbound.appointment.GetAppointmentWaitlistUseCase;
import com.benhsoan.port.inbound.appointment.GetWaitlistSuggestionUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/appointments/waitlist")
@RequiredArgsConstructor
@Validated
public class AppointmentWaitlistController {

    private final AddToWaitlistUseCase addToWaitlistUseCase;
    private final GetAppointmentWaitlistUseCase getAppointmentWaitlistUseCase;
    private final GetWaitlistSuggestionUseCase getWaitlistSuggestionUseCase;
    private final CancelWaitlistEntryUseCase cancelWaitlistEntryUseCase;
    private final AppointmentWaitlistRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("APPOINTMENT_CREATE")
    public AppointmentWaitlistResponse addToWaitlist(@Valid @RequestBody AddToWaitlistRequest request) {
        var result = addToWaitlistUseCase.addToWaitlist(mapper.toCommand(request));
        return mapper.toResponse(result);
    }

    @GetMapping
    @RequirePermission("APPOINTMENT_READ")
    public List<AppointmentWaitlistResponse> getWaitlist(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) WaitlistStatus status
    ) {
        var results = getAppointmentWaitlistUseCase.getWaitlist(
                new GetAppointmentWaitlistQuery(doctorId, date, status)
        );
        return mapper.toResponse(results);
    }

    @GetMapping("/suggest")
    @RequirePermission("APPOINTMENT_READ")
    public ResponseEntity<WaitlistSuggestionResponse> getSuggestion(
            @RequestParam UUID doctorId,
            @RequestParam LocalDate date
    ) {
        var suggestionOpt = getWaitlistSuggestionUseCase.getSuggestion(
                new GetWaitlistSuggestionQuery(doctorId, date)
        );
        return suggestionOpt
                .map(suggestion -> ResponseEntity.ok(mapper.toResponse(suggestion)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/cancel")
    @RequirePermission("APPOINTMENT_UPDATE")
    public AppointmentWaitlistResponse cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelWaitlistRequest request
    ) {
        var result = cancelWaitlistEntryUseCase.cancel(mapper.toCommand(id, request));
        return mapper.toResponse(result);
    }
}

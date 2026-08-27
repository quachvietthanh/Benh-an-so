package com.benhsoan.adapter.inbound.rest.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalAppointmentRestMapper;
import com.benhsoan.adapter.inbound.rest.request.appointment.PatientBookAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.PatientAppointmentResponse;
import com.benhsoan.port.dto.query.appointment.GetDoctorAvailableSlotsQuery;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.inbound.appointment.GetDoctorAvailableSlotsUseCase;
import com.benhsoan.port.inbound.appointment.PatientBookAppointmentUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/patient-portal/appointments")
@RequiredArgsConstructor
public class PatientPortalAppointmentController {

    private final GetDoctorAvailableSlotsUseCase getDoctorAvailableSlotsUseCase;

    private final PatientBookAppointmentUseCase patientBookAppointmentUseCase;

    private final PatientPortalAppointmentRestMapper mapper;

    @GetMapping("/available-slots")
    public List<DoctorAvailableSlotResult> getAvailableSlots(
            @RequestParam UUID doctorId,
            @RequestParam LocalDate date
    ) {
        return getDoctorAvailableSlotsUseCase.getAvailableSlots(
                new GetDoctorAvailableSlotsQuery(doctorId, date)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientAppointmentResponse book(
            @Valid @RequestBody PatientBookAppointmentRequest request
    ) {
        PatientAppointmentResult result =
                patientBookAppointmentUseCase.book(mapper.toCommand(request));

        return mapper.toResponse(result);
    }

}

package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.DoctorScheduleRestMapper;
import com.benhsoan.adapter.inbound.rest.request.appointment.RegisterDoctorTimeOffRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.SetDoctorWeeklyScheduleRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.AffectedAppointmentResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorTimeOffResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyScheduleResponse;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.query.appointment.GetDoctorTimeOffsQuery;
import com.benhsoan.port.dto.query.appointment.GetDoctorWeeklyScheduleQuery;
import com.benhsoan.port.inbound.appointment.CancelDoctorTimeOffUseCase;
import com.benhsoan.port.inbound.appointment.GetAffectedAppointmentsByTimeOffUseCase;
import com.benhsoan.port.inbound.appointment.GetDoctorTimeOffsUseCase;
import com.benhsoan.port.inbound.appointment.GetDoctorWeeklyScheduleUseCase;
import com.benhsoan.port.inbound.appointment.RegisterDoctorTimeOffUseCase;
import com.benhsoan.port.inbound.appointment.SetDoctorWeeklyScheduleUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for managing doctor working schedules and time-off / leave intervals (NCL-03-CN-006).
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Validated
public class DoctorScheduleController {

    private final SetDoctorWeeklyScheduleUseCase setDoctorWeeklyScheduleUseCase;
    private final GetDoctorWeeklyScheduleUseCase getDoctorWeeklyScheduleUseCase;
    private final RegisterDoctorTimeOffUseCase registerDoctorTimeOffUseCase;
    private final GetDoctorTimeOffsUseCase getDoctorTimeOffsUseCase;
    private final CancelDoctorTimeOffUseCase cancelDoctorTimeOffUseCase;
    private final GetAffectedAppointmentsByTimeOffUseCase getAffectedAppointmentsByTimeOffUseCase;
    private final DoctorScheduleRestMapper mapper;

    @PutMapping("/doctor-schedules/weekly")
    @RequirePermission("DOCTOR_SCHEDULE_UPDATE")
    public DoctorWeeklyScheduleResponse setWeeklySchedule(
            @Valid @RequestBody SetDoctorWeeklyScheduleRequest request
    ) {
        return mapper.toResponse(
                setDoctorWeeklyScheduleUseCase.setWeeklySchedule(mapper.toCommand(request))
        );
    }

    @GetMapping("/doctor-schedules/weekly")
    @RequirePermission("DOCTOR_SCHEDULE_READ")
    public DoctorWeeklyScheduleResponse getWeeklySchedule(
            @RequestParam UUID doctorId
    ) {
        return mapper.toResponse(
                getDoctorWeeklyScheduleUseCase.getWeeklySchedule(new GetDoctorWeeklyScheduleQuery(doctorId))
        );
    }

    @PostMapping("/doctor-time-offs")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("DOCTOR_TIME_OFF_CREATE")
    public DoctorTimeOffResponse registerTimeOff(
            @Valid @RequestBody RegisterDoctorTimeOffRequest request
    ) {
        return mapper.toResponse(
                registerDoctorTimeOffUseCase.registerTimeOff(mapper.toCommand(request))
        );
    }

    @GetMapping("/doctor-time-offs")
    @RequirePermission("DOCTOR_TIME_OFF_READ")
    public List<DoctorTimeOffResponse> getTimeOffs(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) TimeOffStatus status,
            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime
    ) {
        return getDoctorTimeOffsUseCase.getTimeOffs(new GetDoctorTimeOffsQuery(doctorId, status, fromTime, toTime)).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @DeleteMapping("/doctor-time-offs/{id}")
    @RequirePermission("DOCTOR_TIME_OFF_DELETE")
    public DoctorTimeOffResponse cancelTimeOff(@PathVariable UUID id) {
        return mapper.toResponse(cancelDoctorTimeOffUseCase.cancelTimeOff(id));
    }

    @GetMapping("/doctor-time-offs/{id}/affected-appointments")
    @RequirePermission("DOCTOR_TIME_OFF_READ")
    public List<AffectedAppointmentResponse> getAffectedAppointments(@PathVariable UUID id) {
        return getAffectedAppointmentsByTimeOffUseCase.getAffectedAppointments(id).stream()
                .map(mapper::toResponse)
                .toList();
    }
}

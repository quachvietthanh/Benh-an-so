package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.DoctorScheduleRestMapper;
import com.benhsoan.adapter.inbound.rest.request.appointment.ConfigureDoctorWeeklyScheduleRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.RegisterDoctorTimeOffRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorTimeOffResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyScheduleResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;
import com.benhsoan.port.inbound.appointment.CancelDoctorTimeOffUseCase;
import com.benhsoan.port.inbound.appointment.ConfigureDoctorWeeklyScheduleUseCase;
import com.benhsoan.port.inbound.appointment.GetDoctorScheduleUseCase;
import com.benhsoan.port.inbound.appointment.GetDoctorTimeOffsUseCase;
import com.benhsoan.port.inbound.appointment.RegisterDoctorTimeOffUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for managing doctor working schedules and unexpected time-offs (NCL-03-CN-006).
 */
@RestController
@RequestMapping("/system/doctors/{doctorId}")
@RequiredArgsConstructor
@Validated
public class DoctorScheduleController {

    private final ConfigureDoctorWeeklyScheduleUseCase configureDoctorWeeklyScheduleUseCase;
    private final GetDoctorScheduleUseCase getDoctorScheduleUseCase;
    private final RegisterDoctorTimeOffUseCase registerDoctorTimeOffUseCase;
    private final GetDoctorTimeOffsUseCase getDoctorTimeOffsUseCase;
    private final CancelDoctorTimeOffUseCase cancelDoctorTimeOffUseCase;
    private final DoctorScheduleRestMapper mapper;

    @GetMapping("/schedules/weekly")
    @RequirePermission("DOCTOR_SCHEDULE_READ")
    public List<DoctorWeeklyScheduleResponse> getWeeklySchedule(@PathVariable UUID doctorId) {
        List<DoctorWeeklyScheduleResult> results = getDoctorScheduleUseCase.getWeeklySchedule(doctorId);
        return mapper.toWeeklyResponseList(results);
    }

    /**
     * Configures the doctor's recurring weekly working schedule (PUT collection replacement semantics).
     * All desired working days must be specified in the request payload.
     * Any existing working days previously configured for this doctor that are omitted from the payload
     * will automatically be deactivated (active = false).
     */
    @PutMapping("/schedules/weekly")
    @RequirePermission("DOCTOR_SCHEDULE_UPDATE")
    public List<DoctorWeeklyScheduleResponse> configureWeeklySchedule(
            @PathVariable UUID doctorId,
            @Valid @RequestBody ConfigureDoctorWeeklyScheduleRequest request
    ) {
        List<DoctorWeeklyScheduleResult> results = configureDoctorWeeklyScheduleUseCase
                .configureWeeklySchedule(mapper.toCommand(doctorId, request));
        return mapper.toWeeklyResponseList(results);
    }

    @PostMapping("/time-offs")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("DOCTOR_TIMEOFF_CREATE")
    public DoctorTimeOffResponse registerTimeOff(
            @PathVariable UUID doctorId,
            @Valid @RequestBody RegisterDoctorTimeOffRequest request
    ) {
        DoctorTimeOffResult result = registerDoctorTimeOffUseCase
                .registerTimeOff(mapper.toCommand(doctorId, request));
        return mapper.toResponse(result);
    }

    @GetMapping("/time-offs")
    @RequirePermission("DOCTOR_TIMEOFF_READ")
    public List<DoctorTimeOffResponse> getTimeOffs(@PathVariable UUID doctorId) {
        List<DoctorTimeOffResult> results = getDoctorTimeOffsUseCase.getTimeOffs(doctorId);
        return mapper.toTimeOffResponseList(results);
    }

    @PatchMapping("/time-offs/{timeOffId}/cancel")
    @RequirePermission("DOCTOR_TIMEOFF_CANCEL")
    public DoctorTimeOffResponse cancelTimeOff(
            @PathVariable UUID doctorId,
            @PathVariable UUID timeOffId
    ) {
        DoctorTimeOffResult result = cancelDoctorTimeOffUseCase.cancelTimeOff(doctorId, timeOffId);
        return mapper.toResponse(result);
    }
}

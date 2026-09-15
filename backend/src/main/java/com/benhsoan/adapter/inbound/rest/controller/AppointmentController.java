package com.benhsoan.adapter.inbound.rest.controller;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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

import com.benhsoan.adapter.inbound.rest.mapper.AppointmentRestMapper;
import com.benhsoan.adapter.inbound.rest.request.appointment.CancelAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.CreateAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentResponse;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.appointment.GetOverdueAppointmentsCommand;
import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.AppointmentReminderResult;
import com.benhsoan.port.inbound.appointment.CancelAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.ConfirmAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.CreateAppointmentUseCase;
import com.benhsoan.port.inbound.appointment.GetAppointmentByIdUseCase;
import com.benhsoan.port.inbound.appointment.GetOverdueAppointmentsUseCase;
import com.benhsoan.port.inbound.appointment.GetUnconfirmedAppointmentsUseCase;
import com.benhsoan.port.inbound.appointment.MarkAppointmentNoShowUseCase;
import com.benhsoan.port.inbound.appointment.SearchAppointmentsUseCase;
import com.benhsoan.port.inbound.appointment.SendAppointmentReminderManuallyUseCase;
import com.benhsoan.adapter.inbound.rest.request.appointment.RescheduleAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse;
import com.benhsoan.port.dto.query.appointment.GetDoctorWeeklyScheduleTableQuery;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult;
import com.benhsoan.port.inbound.appointment.GetDoctorWeeklyScheduleTableUseCase;
import com.benhsoan.port.inbound.appointment.RescheduleAppointmentUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Validated
public class AppointmentController {

    private final CreateAppointmentUseCase createAppointmentUseCase;

    private final CancelAppointmentUseCase cancelAppointmentUseCase;

    private final ConfirmAppointmentUseCase confirmAppointmentUseCase;

    private final MarkAppointmentNoShowUseCase markAppointmentNoShowUseCase;

    private final GetOverdueAppointmentsUseCase getOverdueAppointmentsUseCase;

    private final GetUnconfirmedAppointmentsUseCase getUnconfirmedAppointmentsUseCase;

    private final SearchAppointmentsUseCase searchAppointmentsUseCase;

    private final GetAppointmentByIdUseCase getAppointmentByIdUseCase;

    private final RescheduleAppointmentUseCase rescheduleAppointmentUseCase;

    private final SendAppointmentReminderManuallyUseCase sendAppointmentReminderManuallyUseCase;

    private final GetDoctorWeeklyScheduleTableUseCase getDoctorWeeklyScheduleTableUseCase;

    private final AppointmentRestMapper mapper;

    @GetMapping("/doctor-weekly-table")
    @RequirePermission("APPOINTMENT_READ")
    public DoctorWeeklyTableResponse getDoctorWeeklyTable(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) UUID doctorId) {
        DoctorWeeklyTableResult result = getDoctorWeeklyScheduleTableUseCase.getWeeklyScheduleTable(
                new GetDoctorWeeklyScheduleTableQuery(date, doctorId));
        return mapper.toResponse(result);
    }

    @GetMapping
    @RequirePermission("APPOINTMENT_READ")
    public Page<AppointmentResponse> search(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before end date.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startTime"));
        Page<AppointmentResult> results = searchAppointmentsUseCase.search(
                new SearchAppointmentCommand(patientId, doctorId, status, startDate, endDate, pageable));
        return mapper.toResponse(results);
    }

    @GetMapping("/available-slots")
    @RequirePermission("APPOINTMENT_READ")
    public List<DoctorAvailableSlotResult> getAvailableSlots(
            @RequestParam UUID doctorId,
            @RequestParam LocalDate date) {
        return getDoctorAvailableSlotsUseCase.getAvailableSlots(
                new GetDoctorAvailableSlotsQuery(doctorId, date));
    }

    @GetMapping("/{id}")
    @RequirePermission("APPOINTMENT_READ")
    public AppointmentResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getAppointmentByIdUseCase.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("APPOINTMENT_CREATE")
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResult result = createAppointmentUseCase.create(
                mapper.toCommand(request));
        return mapper.toResponse(result);

    }

    @PostMapping("/{id}/reminder")
    @RequirePermission("APPOINTMENT_UPDATE")
    public AppointmentReminderResult sendReminderManually(@PathVariable UUID id) {
        return sendAppointmentReminderManuallyUseCase.sendManually(id);
    }

    @PatchMapping("/{id}/confirm")
    @RequirePermission("APPOINTMENT_UPDATE")
    public AppointmentResponse confirm(@PathVariable UUID id) {
        AppointmentResult result = confirmAppointmentUseCase.confirm(id);
        return mapper.toResponse(result);
    }

    @PatchMapping("/{id}/cancel")
    @RequirePermission("APPOINTMENT_UPDATE")
    public AppointmentResponse cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelAppointmentRequest request) {

        AppointmentResult result = cancelAppointmentUseCase.cancel(
                id,
                mapper.toCommand(request));
        return mapper.toResponse(result);
    }

    @PatchMapping("/{id}/reschedule")
    @RequirePermission("APPOINTMENT_UPDATE")
    public AppointmentResponse reschedule(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        AppointmentResult result = rescheduleAppointmentUseCase.reschedule(
                id,
                mapper.toCommand(request));
        return mapper.toResponse(result);
    }

    @GetMapping("/unconfirmed")
    @RequirePermission("APPOINTMENT_READ")
    public Page<AppointmentResponse> getUnconfirmed(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startTime"));
        Page<AppointmentResult> result = getUnconfirmedAppointmentsUseCase.getUnconfirmed(date, pageable);
        return mapper.toResponse(result);
    }

    @GetMapping("/overdue")
    @RequirePermission("APPOINTMENT_READ")
    public Page<AppointmentResponse> getOverdueAppointments(Pageable pageable) {
        Page<AppointmentResult> result = getOverdueAppointmentsUseCase.execute(
                GetOverdueAppointmentsCommand.builder()
                        .pageable(pageable)
                        .build());

        return mapper.toResponse(result);
    }

    @PatchMapping("/{id}/no-show")
    @RequirePermission("APPOINTMENT_UPDATE")
    public AppointmentResponse markNoShow(@PathVariable UUID id) {
        AppointmentResult result = markAppointmentNoShowUseCase.execute(
                mapper.toCommand(id));

        return mapper.toResponse(result);
    }

}

package com.benhsoan.adapter.inbound.rest.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.benhsoan.adapter.inbound.rest.mapper.FollowUpReminderRestMapper;
import com.benhsoan.adapter.inbound.rest.request.followup.CreateFollowUpReminderRequest;
import com.benhsoan.adapter.inbound.rest.request.followup.UpdateFollowUpReminderStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.followup.FollowUpReminderResponse;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.followup.SearchFollowUpRemindersQuery;
import com.benhsoan.port.inbound.followup.CreateFollowUpReminderUseCase;
import com.benhsoan.port.inbound.followup.GetDueFollowUpRemindersUseCase;
import com.benhsoan.port.inbound.followup.SearchFollowUpRemindersUseCase;
import com.benhsoan.port.inbound.followup.UpdateFollowUpReminderStatusUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/follow-up-reminders")
@RequiredArgsConstructor
@Validated
public class FollowUpReminderController {

    private final CreateFollowUpReminderUseCase createFollowUpReminderUseCase;
    private final GetDueFollowUpRemindersUseCase getDueFollowUpRemindersUseCase;
    private final SearchFollowUpRemindersUseCase searchFollowUpRemindersUseCase;
    private final UpdateFollowUpReminderStatusUseCase updateFollowUpReminderStatusUseCase;
    private final FollowUpReminderRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("FOLLOW_UP_REMINDER_CREATE")
    public FollowUpReminderResponse create(@Valid @RequestBody CreateFollowUpReminderRequest request) {
        return mapper.toResponse(createFollowUpReminderUseCase.create(mapper.toCommand(request)));
    }

    @GetMapping("/due")
    @RequirePermission("FOLLOW_UP_REMINDER_READ")
    public Page<FollowUpReminderResponse> getDue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validateRange(from, to);
        validatePage(page, size);

        return mapper.toResponse(getDueFollowUpRemindersUseCase.getDue(
                from,
                to,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "remindAt"))
        ));
    }

    @GetMapping
    @RequirePermission("FOLLOW_UP_REMINDER_READ")
    public Page<FollowUpReminderResponse> search(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) ReminderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validateRange(from, to);
        validatePage(page, size);

        return mapper.toResponse(searchFollowUpRemindersUseCase.search(
                new SearchFollowUpRemindersQuery(
                        patientId,
                        status,
                        from,
                        to,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "remindAt"))
                )
        ));
    }

    @PatchMapping("/{id}/status")
    @RequirePermission("FOLLOW_UP_REMINDER_UPDATE")
    public FollowUpReminderResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFollowUpReminderStatusRequest request
    ) {
        return mapper.toResponse(updateFollowUpReminderStatusUseCase.updateStatus(id, request.status()));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("from must be before or equal to to.");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PostCareLogRestMapper;
import com.benhsoan.adapter.inbound.rest.request.carelog.CreatePostCareLogRequest;
import com.benhsoan.adapter.inbound.rest.response.carelog.PostCareLogResponse;
import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.carelog.SearchPostCareLogsQuery;
import com.benhsoan.port.inbound.carelog.CreatePostCareLogUseCase;
import com.benhsoan.port.inbound.carelog.GetPatientCareLogsUseCase;
import com.benhsoan.port.inbound.carelog.SearchPostCareLogsUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/care-logs")
@RequiredArgsConstructor
@Validated
public class PostCareLogController {

    private final CreatePostCareLogUseCase createPostCareLogUseCase;
    private final GetPatientCareLogsUseCase getPatientCareLogsUseCase;
    private final SearchPostCareLogsUseCase searchPostCareLogsUseCase;
    private final PostCareLogRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public PostCareLogResponse create(@Valid @RequestBody CreatePostCareLogRequest request) {
        return mapper.toResponse(createPostCareLogUseCase.create(mapper.toCommand(request)));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public List<PostCareLogResponse> getForPatient(@PathVariable UUID patientId) {
        return mapper.toResponse(getPatientCareLogsUseCase.getForPatient(patientId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public Page<PostCareLogResponse> search(
            @RequestParam(required = false) ContactChannel channel,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validateRange(from, to);
        validatePage(page, size);

        return mapper.toResponse(searchPostCareLogsUseCase.search(
                new SearchPostCareLogsQuery(
                        channel,
                        from,
                        to,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "contactedAt"))
                )
        ));
    }

    private void validateRange(Instant from, Instant to) {
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

package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDetailRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordRestMapper;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.AmendMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.CreateMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordAccessLogResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordAmendmentResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordResponse;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
public class MedicalRecordController {

    private final CreateMedicalRecordUseCase createMedicalRecordUseCase;
    private final GetMedicalRecordUseCase getMedicalRecordUseCase;
    private final UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
    private final LockMedicalRecordUseCase lockMedicalRecordUseCase;
    private final AmendMedicalRecordUseCase amendMedicalRecordUseCase;
    private final GetMedicalRecordAccessLogsUseCase getMedicalRecordAccessLogsUseCase;
    private final MedicalRecordRestMapper mapper;
    private final MedicalRecordDetailRestMapper detailMapper;

    @PostMapping("/medical-records")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public MedicalRecordResponse create(@Valid @RequestBody CreateMedicalRecordRequest request) {
        return mapper.toResponse(createMedicalRecordUseCase.create(mapper.toCommand(request)));
    }

    @GetMapping("/medical-records/{medicalRecordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public MedicalRecordResponse getById(@PathVariable UUID medicalRecordId) {
        return mapper.toResponse(getMedicalRecordUseCase.getById(medicalRecordId));
    }

    @GetMapping("/medical-records/visits/{visitId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public MedicalRecordDetailResponse getByVisitId(@PathVariable UUID visitId) {
        return detailMapper.toResponse(getMedicalRecordUseCase.getDetailByVisitId(visitId));
    }

    @GetMapping("/patients/{patientId}/medical-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public List<MedicalRecordDetailResponse> getPatientMedicalRecords(@PathVariable UUID patientId) {
        return detailMapper.toResponses(getMedicalRecordUseCase.getHistoryByPatientId(patientId));
    }

    @PutMapping("/medical-records/{medicalRecordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public MedicalRecordResponse update(@PathVariable UUID medicalRecordId, @RequestBody UpdateMedicalRecordRequest request) {
        return mapper.toResponse(updateMedicalRecordUseCase.update(medicalRecordId, mapper.toCommand(request)));
    }

    @PostMapping("/medical-records/{medicalRecordId}/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public MedicalRecordResponse lock(@PathVariable UUID medicalRecordId) {
        return mapper.toResponse(lockMedicalRecordUseCase.lock(medicalRecordId));
    }

    @PostMapping("/medical-records/{medicalRecordId}/amendments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public MedicalRecordAmendmentResponse amend(@PathVariable UUID medicalRecordId, @Valid @RequestBody AmendMedicalRecordRequest request) {
        return mapper.toResponse(amendMedicalRecordUseCase.amend(medicalRecordId, mapper.toCommand(request)));
    }

    @GetMapping("/medical-records/{medicalRecordId}/access-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public Page<MedicalRecordAccessLogResponse> getAccessLogsByRecord(
            @PathVariable UUID medicalRecordId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return mapper.toAccessLogResponse(getMedicalRecordAccessLogsUseCase.getAccessLogs(
                mapper.toQuery(medicalRecordId, null, from, to, page, size)
        ));
    }

    @GetMapping("/medical-records/access-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public Page<MedicalRecordAccessLogResponse> getAccessLogsByPatient(
            @RequestParam UUID patientId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return mapper.toAccessLogResponse(getMedicalRecordAccessLogsUseCase.getAccessLogs(
                mapper.toQuery(null, patientId, from, to, page, size)
        ));
    }
}

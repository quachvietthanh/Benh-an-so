package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDetailRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDiagnosisRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordRestMapper;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.AmendMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.CreateMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.IssueMedicalRecordCopyRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.ReplaceMedicalRecordDiagnosesRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordAccessLogResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordAmendmentResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDiagnosisResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordVersionHistoryResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ArchiveMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.DeleteMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordVersionHistoryUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.IssueMedicalRecordCopyUseCase;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.SignMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ReplaceMedicalRecordDiagnosesUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
@Validated
public class MedicalRecordController {

    private final CreateMedicalRecordUseCase createMedicalRecordUseCase;
    private final GetMedicalRecordUseCase getMedicalRecordUseCase;
    private final UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
    private final LockMedicalRecordUseCase lockMedicalRecordUseCase;
    private final SignMedicalRecordUseCase signMedicalRecordUseCase;
    private final ArchiveMedicalRecordUseCase archiveMedicalRecordUseCase;
    private final DeleteMedicalRecordUseCase deleteMedicalRecordUseCase;
    private final AmendMedicalRecordUseCase amendMedicalRecordUseCase;
    private final GetMedicalRecordAccessLogsUseCase getMedicalRecordAccessLogsUseCase;
    private final GetMedicalRecordDiagnosesUseCase getMedicalRecordDiagnosesUseCase;
    private final ReplaceMedicalRecordDiagnosesUseCase replaceMedicalRecordDiagnosesUseCase;
    private final IssueMedicalRecordCopyUseCase issueMedicalRecordCopyUseCase;
    private final GetMedicalRecordVersionHistoryUseCase getMedicalRecordVersionHistoryUseCase;
    private final MedicalRecordRestMapper mapper;
    private final MedicalRecordDetailRestMapper detailMapper;
    private final MedicalRecordDiagnosisRestMapper diagnosisMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("MEDICAL_RECORD_CREATE")
    public MedicalRecordResponse create(@Valid @RequestBody CreateMedicalRecordRequest request) {
        return mapper.toResponse(createMedicalRecordUseCase.create(mapper.toCommand(request)));
    }

    @GetMapping("/{medicalRecordId}")
    @RequirePermission("MEDICAL_RECORD_READ")
    public MedicalRecordResponse getById(@PathVariable UUID medicalRecordId) {
        return mapper.toResponse(getMedicalRecordUseCase.getById(medicalRecordId));
    }

    @GetMapping("/{medicalRecordId}/diagnoses")
    @RequirePermission("MEDICAL_RECORD_READ")
    public List<MedicalRecordDiagnosisResponse> getDiagnoses(@PathVariable UUID medicalRecordId) {
        return diagnosisMapper.toResponses(getMedicalRecordDiagnosesUseCase.getByMedicalRecordId(medicalRecordId));
    }

    @GetMapping("/visits/{visitId}")
    @RequirePermission("MEDICAL_RECORD_READ")
    public MedicalRecordDetailResponse getByVisitId(@PathVariable UUID visitId) {
        return detailMapper.toResponse(getMedicalRecordUseCase.getDetailByVisitId(visitId));
    }

    @GetMapping("/patient/{patientId}")
    @RequirePermission("MEDICAL_RECORD_READ")
    public List<MedicalRecordDetailResponse> getPatientMedicalRecords(@PathVariable UUID patientId) {
        return detailMapper.toResponses(getMedicalRecordUseCase.getHistoryByPatientId(patientId));
    }

    @PutMapping("/{medicalRecordId}")
    @RequirePermission("MEDICAL_RECORD_UPDATE")
    public MedicalRecordResponse update(@PathVariable UUID medicalRecordId, @RequestBody UpdateMedicalRecordRequest request) {
        return mapper.toResponse(updateMedicalRecordUseCase.update(medicalRecordId, mapper.toCommand(request)));
    }

    @PutMapping("/{medicalRecordId}/diagnoses")
    @RequirePermission("MEDICAL_RECORD_UPDATE")
    public List<MedicalRecordDiagnosisResponse> replaceDiagnoses(
            @PathVariable UUID medicalRecordId,
            @Valid @RequestBody ReplaceMedicalRecordDiagnosesRequest request
    ) {
        return diagnosisMapper.toResponses(replaceMedicalRecordDiagnosesUseCase.replace(
                medicalRecordId, diagnosisMapper.toCommand(request)
        ));
    }

    @PostMapping("/{medicalRecordId}/sign")
    @RequirePermission("MEDICAL_RECORD_UPDATE_STATUS")
    public MedicalRecordResponse sign(
            @PathVariable UUID medicalRecordId,
            @RequestBody(required = false) com.benhsoan.adapter.inbound.rest.request.medicalrecord.SignMedicalRecordRequest request
    ) {
        return mapper.toResponse(signMedicalRecordUseCase.sign(medicalRecordId, mapper.toCommand(request)));
    }

    @PostMapping("/{medicalRecordId}/lock")
    @RequirePermission("MEDICAL_RECORD_UPDATE_STATUS")
    public MedicalRecordResponse lock(@PathVariable UUID medicalRecordId) {
        return mapper.toResponse(lockMedicalRecordUseCase.lock(medicalRecordId));
    }

    @PostMapping("/{medicalRecordId}/archive")
    @RequirePermission("MEDICAL_RECORD_UPDATE_STATUS")
    public MedicalRecordResponse archive(@PathVariable UUID medicalRecordId) {
        return mapper.toResponse(archiveMedicalRecordUseCase.archive(medicalRecordId));
    }

    @PostMapping("/{medicalRecordId}/copy")
    @RequirePermission("MEDICAL_RECORD_COPY")
    public ResponseEntity<ByteArrayResource> issueCopy(
            @PathVariable UUID medicalRecordId,
            @Valid @RequestBody IssueMedicalRecordCopyRequest request
    ) {
        var result = issueMedicalRecordCopyUseCase.issue(mapper.toCommand(medicalRecordId, request));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.content().length)
                .body(new ByteArrayResource(result.content()));
    }

    @DeleteMapping("/{medicalRecordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission("MEDICAL_RECORD_DELETE")
    public void delete(@PathVariable UUID medicalRecordId) {
        deleteMedicalRecordUseCase.delete(medicalRecordId);
    }

    @PostMapping("/{medicalRecordId}/amendments")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("MEDICAL_RECORD_UPDATE")
    public MedicalRecordAmendmentResponse amend(@PathVariable UUID medicalRecordId, @Valid @RequestBody AmendMedicalRecordRequest request) {
        return mapper.toResponse(amendMedicalRecordUseCase.amend(medicalRecordId, mapper.toCommand(request)));
    }

    @GetMapping("/{medicalRecordId}/versions")
    @RequirePermission("MEDICAL_RECORD_READ")
    public MedicalRecordVersionHistoryResponse getVersionHistory(@PathVariable UUID medicalRecordId) {
        return mapper.toResponse(getMedicalRecordVersionHistoryUseCase.getVersionHistory(medicalRecordId));
    }

    @GetMapping("/{medicalRecordId}/access-logs")
    @RequirePermission("AUDIT_READ")
    public Page<MedicalRecordAccessLogResponse> getAccessLogsByRecord(
            @PathVariable UUID medicalRecordId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return mapper.toAccessLogResponse(getMedicalRecordAccessLogsUseCase.getAccessLogs(
                mapper.toQuery(null, null, medicalRecordId, null, from, to, page, size)
        ));
    }

    @GetMapping("/access-logs")
    @RequirePermission("AUDIT_READ")
    public Page<MedicalRecordAccessLogResponse> getAccessLogsByPatient(
            @RequestParam UUID patientId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return mapper.toAccessLogResponse(getMedicalRecordAccessLogsUseCase.getAccessLogs(
                mapper.toQuery(null, patientId, null, null, from, to, page, size)
        ));
    }
}

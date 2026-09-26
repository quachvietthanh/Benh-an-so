package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.benhsoan.adapter.inbound.rest.mapper.PatientRestMapper;
import com.benhsoan.adapter.inbound.rest.request.patient.MergePatientsRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.RegisterPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.SearchPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientPregnancyStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.DuplicatePatientGroupResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.MergePatientsResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientImportLogResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientImportPreviewResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientImportResultResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.result.patient.MergePatientsResult;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.DownloadPatientImportTemplateUseCase;
import com.benhsoan.port.inbound.patient.FindDuplicatePatientsUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByCodeUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByIdUseCase;
import com.benhsoan.port.inbound.patient.GetPatientImportLogsUseCase;
import com.benhsoan.port.inbound.patient.ImportPatientsUseCase;
import com.benhsoan.port.inbound.patient.MergePatientsUseCase;
import com.benhsoan.port.inbound.patient.PreviewPatientImportUseCase;
import com.benhsoan.port.inbound.patient.RegisterPatientUseCase;
import com.benhsoan.port.inbound.patient.SearchPatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientPregnancyStatusUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Validated
public class PatientController {

    private final RegisterPatientUseCase registerPatientUseCase;

    private final SearchPatientUseCase searchPatientUseCase;

    private final UpdatePatientUseCase updatePatientUseCase;

    private final GetPatientByIdUseCase getPatientByIdUseCase;

    private final GetPatientByCodeUseCase getPatientByCodeUseCase;

    private final MergePatientsUseCase mergePatientsUseCase;

    private final FindDuplicatePatientsUseCase findDuplicatePatientsUseCase;

    private final UpdatePatientPregnancyStatusUseCase updatePatientPregnancyStatusUseCase;

    private final DownloadPatientImportTemplateUseCase downloadPatientImportTemplateUseCase;

    private final PreviewPatientImportUseCase previewPatientImportUseCase;

    private final ImportPatientsUseCase importPatientsUseCase;

    private final GetPatientImportLogsUseCase getPatientImportLogsUseCase;

    private final PatientRestMapper patientRestMapper;

    @PostMapping
    @RequirePermission("PATIENT_CREATE")
    public PatientResponse register(
            @Valid @RequestBody RegisterPatientRequest request
    ) {

        PatientResult result =
                registerPatientUseCase.register(
                        patientRestMapper.toCommand(request));

        return patientRestMapper.toResponse(result);
    }

    @GetMapping
    @RequirePermission("PATIENT_READ")
    public Page<PatientResponse> search(
        SearchPatientRequest request,
        Pageable pageable ) {
                return patientRestMapper.toResponse( 
                        searchPatientUseCase.search(
                                patientRestMapper.toCommand(
                                        request,
                                        pageable
                                )
                        )
                );
        }

    @GetMapping("/code/{code}")
    @RequirePermission("PATIENT_READ")
    public PatientResponse getByCode(@PathVariable String code) {
        return patientRestMapper.toResponse(getPatientByCodeUseCase.getByCode(code));
    }

    @GetMapping("/{patientId}")
    @RequirePermission("PATIENT_READ")
    public PatientResponse getById(@PathVariable UUID patientId) {
        return patientRestMapper.toResponse(getPatientByIdUseCase.getById(patientId));
    }

    @PutMapping("/{patientId}")
    @RequirePermission("PATIENT_UPDATE")
    public PatientResponse update(

            @PathVariable
            UUID patientId,

            @Valid
            @RequestBody
            UpdatePatientRequest request

    ) {

        PatientResult result =
                updatePatientUseCase.update(
                        patientId,
                        patientRestMapper.toCommand(request));

        return patientRestMapper.toResponse(result);
    }

    @PutMapping("/{patientId}/consent")
    @RequirePermission("PATIENT_CONSENT_UPDATE")
    public PatientResponse updateConsent(
            @PathVariable
            UUID patientId,

            @Valid
            @RequestBody
            com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientConsentRequest request
    ) {
        PatientResult result =
                updatePatientUseCase.update(
                        patientId,
                        patientRestMapper.toConsentCommand(request));

        return patientRestMapper.toResponse(result);
    }

    @PatchMapping("/{patientId}/pregnancy-status")
    @RequirePermission("PATIENT_UPDATE")
    public PatientResponse updatePregnancyStatus(
            @PathVariable UUID patientId,
            @Valid @RequestBody UpdatePatientPregnancyStatusRequest request
    ) {
        return patientRestMapper.toResponse(
                updatePatientPregnancyStatusUseCase.update(
                        patientId,
                        request.pregnancyStatus()));
    }

    @PostMapping("/merge")
    @RequirePermission("PATIENT_MERGE")
    public MergePatientsResponse mergePatients(
            @Valid @RequestBody MergePatientsRequest request
    ) {
        MergePatientsResult result =
                mergePatientsUseCase.merge(
                        patientRestMapper.toMergeCommand(request));

        return patientRestMapper.toMergeResponse(result);
    }

    @GetMapping("/duplicates")
    @RequirePermission("PATIENT_READ")
    public List<DuplicatePatientGroupResponse> findDuplicates() {
        return findDuplicatePatientsUseCase.findDuplicates().stream()
                .map(patientRestMapper::toDuplicateGroupResponse)
                .toList();
    }

    @GetMapping("/import/template")
    @RequirePermission("PATIENT_IMPORT")
    public ResponseEntity<ByteArrayResource> downloadImportTemplate() {
        byte[] content = downloadPatientImportTemplateUseCase.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau_danh_sach_benh_nhan.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("PATIENT_IMPORT")
    public PatientImportPreviewResponse previewImport(
            @RequestParam("file") MultipartFile file
    ) {
        validateSpreadsheetFile(file);
        try {
            var result = previewPatientImportUseCase.preview(
                    new com.benhsoan.port.dto.command.patient.PreviewPatientImportCommand(
                            file.getBytes(),
                            file.getOriginalFilename() != null ? file.getOriginalFilename() : "danh_sach_benh_nhan.xlsx",
                            file.getSize()
                    )
            );
            return patientRestMapper.toResponse(result);
        } catch (java.io.IOException e) {
            throw new com.benhsoan.domain.shared.exception.ValidationException("Không thể đọc tệp tải lên: " + e.getMessage());
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("PATIENT_IMPORT")
    public PatientImportResultResponse importPatients(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "skipDuplicates", defaultValue = "true") boolean skipDuplicates
    ) {
        validateSpreadsheetFile(file);
        try {
            var result = importPatientsUseCase.importPatients(
                    new com.benhsoan.port.dto.command.patient.ImportPatientsCommand(
                            file.getBytes(),
                            file.getOriginalFilename() != null ? file.getOriginalFilename() : "danh_sach_benh_nhan.xlsx",
                            file.getSize(),
                            skipDuplicates
                    )
            );
            return patientRestMapper.toResponse(result);
        } catch (java.io.IOException e) {
            throw new com.benhsoan.domain.shared.exception.ValidationException("Không thể đọc tệp tải lên: " + e.getMessage());
        }
    }

    @GetMapping("/import-logs")
    @RequirePermission("PATIENT_IMPORT")
    public Page<PatientImportLogResponse> getImportLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return getPatientImportLogsUseCase.getLogs(pageable)
                .map(patientRestMapper::toResponse);
    }

    @GetMapping("/import-logs/{id}")
    @RequirePermission("PATIENT_IMPORT")
    public PatientImportLogResponse getImportLogById(@PathVariable UUID id) {
        return patientRestMapper.toResponse(getPatientImportLogsUseCase.getLogById(id));
    }

    private void validateSpreadsheetFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.benhsoan.domain.shared.exception.ValidationException("Tệp tải lên không được để trống.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            throw new com.benhsoan.domain.shared.exception.ValidationException("Định dạng tệp không hợp lệ. Chỉ chấp nhận tệp bảng tính Excel (.xlsx hoặc .xls).");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new com.benhsoan.domain.shared.exception.ValidationException("Kích thước tệp vượt quá giới hạn tối đa 10MB.");
        }
    }

}


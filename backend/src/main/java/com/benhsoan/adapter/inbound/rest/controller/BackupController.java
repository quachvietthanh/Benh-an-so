package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.BackupRestMapper;
import com.benhsoan.adapter.inbound.rest.request.backup.CreateBackupRequest;
import com.benhsoan.adapter.inbound.rest.response.backup.BackupResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.result.BackupDownloadResult;
import com.benhsoan.port.inbound.backup.CreateBackupUseCase;
import com.benhsoan.port.inbound.backup.DownloadBackupUseCase;
import com.benhsoan.port.inbound.backup.GetBackupByIdUseCase;
import com.benhsoan.port.inbound.backup.ListBackupsUseCase;
import com.benhsoan.port.inbound.backup.RestoreBackupUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backups")
@RequiredArgsConstructor
@Validated
public class BackupController {

    private static final String DEFAULT_DOWNLOAD_CONTENT_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    private final CreateBackupUseCase createBackupUseCase;
    private final ListBackupsUseCase listBackupsUseCase;
    private final GetBackupByIdUseCase getBackupByIdUseCase;
    private final RestoreBackupUseCase restoreBackupUseCase;
    private final DownloadBackupUseCase downloadBackupUseCase;
    private final BackupRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("BACKUP_CREATE")
    public BackupResponse create(@Valid @RequestBody CreateBackupRequest request) {
        return mapper.toResponse(createBackupUseCase.create(mapper.toCommand(request)));
    }

    @GetMapping
    @RequirePermission("BACKUP_READ")
    public List<BackupResponse> list() {
        return mapper.toResponse(listBackupsUseCase.list());
    }

    @GetMapping("/{id}")
    @RequirePermission("BACKUP_READ")
    public BackupResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getBackupByIdUseCase.getById(id));
    }

    @PostMapping("/{id}/restore")
    @RequirePermission("BACKUP_RESTORE")
    public BackupResponse restore(@PathVariable UUID id) {
        return mapper.toResponse(restoreBackupUseCase.restore(id));
    }

    @GetMapping("/{id}/download")
    @RequirePermission("BACKUP_READ")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        BackupDownloadResult result = downloadBackupUseCase.download(id);

        String contentType = (result.contentType() == null || result.contentType().isBlank())
                ? DEFAULT_DOWNLOAD_CONTENT_TYPE
                : result.contentType();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(result.content());
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalHistoryRestMapper;
import com.benhsoan.adapter.inbound.rest.response.patient.MedicalHistoryItemResponse;
import com.benhsoan.application.ucservice.queries.medicalrecord.GetPatientMedicalHistoryQueryHandler;
import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/patients/{patientId}/medical-history")
@RequiredArgsConstructor
public class MedicalHistoryController {

    private final GetPatientMedicalHistoryQueryHandler handler;

    private final MedicalHistoryRestMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<Page<MedicalHistoryItemResponse>> getMedicalHistory(

            @PathVariable
            UUID patientId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant toDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size

    ) {

        GetPatientMedicalHistoryQuery query = GetPatientMedicalHistoryQuery.of(
                patientId, fromDate, toDate, page, size
        );

        return ResponseEntity.ok(
                mapper.toResponse(handler.handle(query))
        );
    }
}

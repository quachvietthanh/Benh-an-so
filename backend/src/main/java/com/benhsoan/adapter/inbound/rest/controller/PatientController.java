package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDetailRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.PatientRestMapper;
import com.benhsoan.adapter.inbound.rest.request.patient.RegisterPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.SearchPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientResponse;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.patient.RegisterPatientUseCase;
import com.benhsoan.port.inbound.patient.SearchPatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientUseCase;

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

    private final GetMedicalRecordUseCase getMedicalRecordUseCase;

    private final MedicalRecordDetailRestMapper medicalRecordDetailRestMapper;

    private final PatientRestMapper patientRestMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public PatientResponse register(
            @Valid @RequestBody RegisterPatientRequest request
    ) {

        PatientResult result =
                registerPatientUseCase.register(
                        patientRestMapper.toCommand(request));

        return patientRestMapper.toResponse(result);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
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

    @PutMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
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

    @GetMapping("/{patientId}/medical-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public List<MedicalRecordDetailResponse> getPatientMedicalRecords(
            @PathVariable UUID patientId
    ) {
        return medicalRecordDetailRestMapper.toResponses(
                getMedicalRecordUseCase.getHistoryByPatientId(patientId)
        );
    }

}

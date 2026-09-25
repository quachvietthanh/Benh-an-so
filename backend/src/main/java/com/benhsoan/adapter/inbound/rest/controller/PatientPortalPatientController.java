package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientLinkedProfileRestMapper;
import com.benhsoan.adapter.inbound.rest.response.patient.LinkedPatientProfileResponse;
import com.benhsoan.port.inbound.patient.GetPatientLinkedProfilesUseCase;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-010 CV-02: patient portal endpoint listing every patient profile the
 * authenticated account may act for (own profile plus linked dependents). Guarded by the
 * existing {@code /patient-portal/**} rule = ROLE_PATIENT, and scoped by the server-side
 * identity only.
 */
@RestController
@RequestMapping("/patient-portal/patients")
@RequiredArgsConstructor
public class PatientPortalPatientController {

    private final GetPatientLinkedProfilesUseCase getPatientLinkedProfilesUseCase;

    private final PatientLinkedProfileRestMapper mapper;

    @GetMapping("/linked")
    public List<LinkedPatientProfileResponse> getLinkedProfiles() {
        return getPatientLinkedProfilesUseCase.getLinkedProfiles().stream()
                .map(mapper::toResponse)
                .toList();
    }
}

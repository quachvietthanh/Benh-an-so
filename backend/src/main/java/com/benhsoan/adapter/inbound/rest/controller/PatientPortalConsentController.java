package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientConsentRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.PatientRestMapper;
import com.benhsoan.adapter.inbound.rest.request.patient.RequestPatientDataErasureRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientConsentRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.DataErasureResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientConsentHistoryResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientResponse;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.GetPatientConsentHistoryUseCase;
import com.benhsoan.port.inbound.patient.RequestPatientDataErasureUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientConsentUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller cho Cổng bệnh nhân (Patient Portal) quản lý phiếu đồng ý cá nhân
 * (NCL-15-CN-005: Bệnh nhân tự kiểm soát dữ liệu cá nhân của mình).
 */
@RestController
@RequestMapping("/patient-portal/consent")
@RequiredArgsConstructor
@Validated
public class PatientPortalConsentController {

    private final CurrentUserPort currentUserPort;
    private final PatientRepository patientRepository;
    private final GetPatientConsentHistoryUseCase getPatientConsentHistoryUseCase;
    private final UpdatePatientConsentUseCase updatePatientConsentUseCase;
    private final RequestPatientDataErasureUseCase requestPatientDataErasureUseCase;
    private final PatientConsentRestMapper consentMapper;
    private final PatientRestMapper patientMapper;

    private Patient getCurrentPatient() {
        UUID userId = currentUserPort.getCurrentUserId();
        if (userId == null) {
            throw new AccessDeniedException("Yêu cầu đăng nhập.");
        }
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientNotFoundException("Không tìm thấy hồ sơ người bệnh của tài khoản."));
    }

    @GetMapping("/history")
    public List<PatientConsentHistoryResponse> getConsentHistory() {
        Patient patient = getCurrentPatient();
        return getPatientConsentHistoryUseCase.getConsentHistory(patient.getId()).stream()
                .map(consentMapper::toHistoryResponse)
                .toList();
    }

    @PutMapping
    public PatientResponse updateConsent(@Valid @RequestBody UpdatePatientConsentRequest request) {
        Patient patient = getCurrentPatient();
        PatientResult result = updatePatientConsentUseCase.updateConsent(
                patient.getId(),
                consentMapper.toCommand(request)
        );
        return patientMapper.toResponse(result);
    }

    @PostMapping("/data-erasure-request")
    public DataErasureResponse requestDataErasure(
            @Valid @RequestBody(required = false) RequestPatientDataErasureRequest request
    ) {
        Patient patient = getCurrentPatient();
        return consentMapper.toErasureResponse(
                requestPatientDataErasureUseCase.requestErasure(
                        patient.getId(),
                        consentMapper.toErasureCommand(request)
                )
        );
    }
}

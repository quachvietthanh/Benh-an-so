package com.benhsoan.application.ucservice.patient;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientConsentRecord;
import com.benhsoan.domain.patient.PatientConsentVersion;
import com.benhsoan.domain.patient.enums.ConsentHistoryStatus;
import com.benhsoan.domain.patient.enums.ConsentScope;
import com.benhsoan.domain.patient.exception.PatientConsentAccessDeniedException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.result.patient.PatientConsentHistoryResult;
import com.benhsoan.port.inbound.patient.GetPatientConsentHistoryUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientConsentHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * Service tra cứu lịch sử các phiên bản phiếu đồng ý theo thứ tự thời gian (NCL-15-CN-005 / AC-02).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientConsentHistoryService implements GetPatientConsentHistoryUseCase {

    private final PatientRepository patientRepository;
    private final PatientConsentHistoryRepository patientConsentHistoryRepository;
    private final CurrentUserPort currentUserPort;
    private final PatientAccessGuard patientAccessGuard;

    @Override
    public List<PatientConsentHistoryResult> getConsentHistory(UUID patientId) {
        // Phân quyền: Lễ tân / Bác sĩ / Admin có PATIENT_READ hoặc PATIENT_CONSENT_UPDATE, hoặc Bệnh nhân sở hữu tài khoản
        boolean isStaff = currentUserPort.hasPermission("PATIENT_READ")
                || currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE");

        if (!isStaff) {
            try {
                patientAccessGuard.requirePatientOwnership(patientId);
            } catch (AccessDeniedException e) {
                throw new PatientConsentAccessDeniedException();
            }
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Không tìm thấy thông tin bệnh nhân: " + patientId));

        List<PatientConsentRecord> history = patientConsentHistoryRepository.findByPatientId(patientId);

        // Trường hợp hồ sơ chưa có bản ghi trong bảng history nhưng đã có consent phẳng
        if (history.isEmpty() && (patient.isConsentAgreed() || patient.getConsentAgreedAt() != null)) {
            ConsentHistoryStatus status = patient.isConsentWithdrawn()
                    ? ConsentHistoryStatus.WITHDRAWN
                    : (patient.isNonMedicalUseRestricted() ? ConsentHistoryStatus.PARTIALLY_WITHDRAWN : ConsentHistoryStatus.AGREED);

            Set<ConsentScope> scopes = patient.isConsentWithdrawn()
                    ? Collections.emptySet()
                    : (patient.isNonMedicalUseRestricted() ? EnumSet.of(ConsentScope.TREATMENT) : ConsentScope.defaultAll());

            PatientConsentRecord defaultRecord = PatientConsentRecord.restore(
                    UUID.randomUUID(),
                    patient.getId(),
                    1,
                    patient.getConsentVersion() != null ? patient.getConsentVersion() : PatientConsentVersion.current(),
                    status,
                    scopes,
                    patient.isConsentAgreed(),
                    patient.getConsentAgreedAt(),
                    patient.isConsentWithdrawn(),
                    patient.getConsentWithdrawnAt(),
                    patient.getConsentWithdrawnReason(),
                    patient.isNonMedicalUseRestricted(),
                    patient.getConsentSignerName(),
                    patient.getCreatedBy(),
                    patient.getConsentAgreedAt() != null ? patient.getConsentAgreedAt() : patient.getCreatedAt()
            );
            history = List.of(defaultRecord);
        }

        return history.stream()
                .map(this::toResult)
                .toList();
    }

    private PatientConsentHistoryResult toResult(PatientConsentRecord record) {
        return PatientConsentHistoryResult.builder()
                .id(record.getId())
                .patientId(record.getPatientId())
                .versionNumber(record.getVersionNumber())
                .versionCode(record.getVersionCode())
                .status(record.getStatus())
                .scopes(record.getScopes())
                .consentAgreed(record.isConsentAgreed())
                .consentAgreedAt(record.getConsentAgreedAt())
                .consentWithdrawn(record.isConsentWithdrawn())
                .consentWithdrawnAt(record.getConsentWithdrawnAt())
                .consentWithdrawnReason(record.getConsentWithdrawnReason())
                .nonMedicalUseRestricted(record.isNonMedicalUseRestricted())
                .signerName(record.getSignerName())
                .createdBy(record.getCreatedBy())
                .createdAt(record.getCreatedAt())
                .build();
    }
}

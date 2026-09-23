package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.PatientConsentRecord;
import com.benhsoan.domain.patient.PatientConsentVersion;
import com.benhsoan.domain.patient.enums.ConsentHistoryStatus;
import com.benhsoan.domain.patient.enums.ConsentScope;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.exception.PatientConsentAccessDeniedException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.UpdatePatientConsentCommand;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.UpdatePatientConsentUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientConsentHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý rút lại và cập nhật phạm vi phiếu đồng ý (NCL-15-CN-005 / QTN-24 / AC-01, AC-02).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdatePatientConsentService implements UpdatePatientConsentUseCase {

    private final PatientRepository patientRepository;
    private final PatientConsentHistoryRepository patientConsentHistoryRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final PatientChangeLogRepository patientChangeLogRepository;
    private final PatientChangeDetailBuilder changeDetailBuilder;
    private final PatientResultMapper patientResultMapper;
    private final PatientAccessGuard patientAccessGuard;

    @Override
    public PatientResult updateConsent(UUID patientId, UpdatePatientConsentCommand command) {
        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort != null ? clockPort.now() : Instant.now();

        // 1. Phân quyền: Lễ tân / Admin có PATIENT_CONSENT_UPDATE, hoặc Bệnh nhân sở hữu tài khoản portal
        boolean isStaff = currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE");
        if (!isStaff) {
            try {
                patientAccessGuard.requirePatientOwnership(patientId);
            } catch (Exception e) {
                throw new PatientConsentAccessDeniedException();
            }
        }

        // 2. Tìm hồ sơ bệnh nhân (Khóa bi quan để bảo vệ thứ tự phiên bản)
        Patient patient = patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Không tìm thấy thông tin bệnh nhân: " + patientId));

        patient.validateCanBeUpdated();

        // Điều kiện tiên quyết: Bệnh nhân đã có phiếu đồng ý trong hệ thống
        if (!patient.isConsentAgreed() && !Boolean.TRUE.equals(command.consentAgreed())) {
            throw new ValidationException("Bệnh nhân chưa có phiếu đồng ý đang hiệu lực.");
        }

        // Validation tính hợp lệ của tham số
        if (Boolean.FALSE.equals(command.consentAgreed())
                && !Boolean.TRUE.equals(command.consentWithdrawn())) {
            throw new ValidationException(
                    "Khi consentAgreed=false, bắt buộc phải có consentWithdrawn=true để rút lại sự đồng ý."
            );
        }

        // Ràng buộc nghiệp vụ: Bắt buộc phải có phạm vi TREATMENT khi duy trì đồng ý
        if (!Boolean.TRUE.equals(command.consentWithdrawn()) && command.scopes() != null) {
            if (command.scopes().isEmpty() || !command.scopes().contains(ConsentScope.TREATMENT)) {
                throw new ValidationException(
                        "Phạm vi xử lý dữ liệu phục vụ khám chữa bệnh (TREATMENT) là bắt buộc khi duy trì đồng ý xử lý dữ liệu."
                );
            }
        }

        Patient oldPatient = Patient.restore(
                patient.getId(),
                patient.getPatientCode(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getIdentityNumber(),
                patient.getInsuranceNumber(),
                patient.getBloodType(),
                patient.getEmergencyContact(),
                patient.getEmergencyRelationship(),
                patient.getEmergencyPhone(),
                patient.getGuardianName(),
                patient.getGuardianRelationship(),
                patient.getGuardianPhone(),
                patient.getGuardianIdentityNumber(),
                patient.getGuardianUserId(),
                patient.getConsentSignerName(),
                patient.isActive(),
                patient.getCreatedAt(),
                patient.getUpdatedAt(),
                patient.getUserId(),
                patient.getCreatedBy(),
                patient.isConsentAgreed(),
                patient.getConsentAgreedAt(),
                patient.getConsentVersion(),
                patient.isConsentWithdrawn(),
                patient.getConsentWithdrawnAt(),
                patient.getConsentWithdrawnReason(),
                patient.isNonMedicalUseRestricted()
        );

        ConsentHistoryStatus historyStatus;
        Set<ConsentScope> effectiveScopes;
        String reason = command.consentWithdrawnReason();

        if (Boolean.TRUE.equals(command.consentWithdrawn())) {
            // Trường hợp 1: Rút lại toàn bộ sự đồng ý
            patient.withdrawConsent(reason, now);
            historyStatus = ConsentHistoryStatus.WITHDRAWN;
            effectiveScopes = Collections.emptySet();
        } else if (Boolean.FALSE.equals(command.consentWithdrawn()) && patient.isConsentWithdrawn()) {
            // Trường hợp 2: Gia hạn / kích hoạt lại sau khi đã rút
            if (!Boolean.TRUE.equals(command.consentAgreed())) {
                throw new ValidationException(
                        "Phải ghi nhận sự đồng ý mới trước khi gia hạn xử lý dữ liệu cá nhân (QTN-24)."
                );
            }
            String v = command.consentVersion() != null
                    ? PatientConsentVersion.requireSupported(command.consentVersion())
                    : (patient.getConsentVersion() != null ? patient.getConsentVersion() : PatientConsentVersion.current());
            patient.renewConsent(v, now);

            Set<ConsentScope> requestedScopes = (command.scopes() != null && !command.scopes().isEmpty())
                    ? command.scopes()
                    : ConsentScope.defaultAll();

            boolean nonMedicalRestricted = !requestedScopes.contains(ConsentScope.COMMUNICATION)
                    && !requestedScopes.contains(ConsentScope.RESEARCH);
            patient.updateConsentScope(nonMedicalRestricted, now);

            effectiveScopes = requestedScopes;
            historyStatus = nonMedicalRestricted ? ConsentHistoryStatus.PARTIALLY_WITHDRAWN : ConsentHistoryStatus.AGREED;
        } else if (command.scopes() != null) {
            // Trường hợp 3: Thu hẹp phạm vi đồng ý (NCL-15-CN-005-TC-01)
            Set<ConsentScope> requestedScopes = command.scopes();
            boolean nonMedicalRestricted = !requestedScopes.contains(ConsentScope.COMMUNICATION)
                    && !requestedScopes.contains(ConsentScope.RESEARCH);

            patient.updateConsentScope(nonMedicalRestricted, now);
            effectiveScopes = requestedScopes;
            historyStatus = (requestedScopes.containsAll(ConsentScope.defaultAll()))
                    ? ConsentHistoryStatus.AGREED
                    : ConsentHistoryStatus.PARTIALLY_WITHDRAWN;
        } else {
            // Giữ nguyên trạng thái / cập nhật version
            if (command.consentVersion() != null) {
                patient.renewConsent(PatientConsentVersion.requireSupported(command.consentVersion()), now);
            }
            historyStatus = patient.isNonMedicalUseRestricted()
                    ? ConsentHistoryStatus.PARTIALLY_WITHDRAWN
                    : ConsentHistoryStatus.AGREED;
            effectiveScopes = patient.isNonMedicalUseRestricted()
                    ? EnumSet.of(ConsentScope.TREATMENT)
                    : ConsentScope.defaultAll();
        }

        // Tạo bản ghi lịch sử phiên bản mới (AC-01, AC-02)
        int nextVersionNumber = patientConsentHistoryRepository.getNextVersionNumber(patientId);
        String versionCode = patient.getConsentVersion() != null ? patient.getConsentVersion() : PatientConsentVersion.current();

        PatientConsentRecord historyRecord = PatientConsentRecord.create(
                patientId,
                nextVersionNumber,
                versionCode,
                historyStatus,
                effectiveScopes,
                patient.isConsentAgreed(),
                patient.getConsentAgreedAt(),
                patient.isConsentWithdrawn(),
                patient.getConsentWithdrawnAt(),
                patient.getConsentWithdrawnReason(),
                patient.isNonMedicalUseRestricted(),
                patient.getConsentSignerName(),
                currentUserId,
                now
        );
        patientConsentHistoryRepository.save(historyRecord);

        // Lưu cập nhật thông tin bệnh nhân
        Patient updatedPatient = patientRepository.save(patient);

        // Ghi nhật ký thay đổi thông tin bệnh nhân (PatientChangeLog)
        String detail = changeDetailBuilder.forUpdate(oldPatient, updatedPatient);
        PatientChangeLog log = PatientChangeLog.create(
                updatedPatient.getId(),
                currentUserId,
                PatientChangeAction.UPDATE,
                detail
        );
        patientChangeLogRepository.save(log);

        // Ghi nhật ký kiểm toán hệ thống (AuditLog)
        auditLogRepository.save(
                AuditLog.create(
                        currentUserId,
                        ActionType.UPDATE,
                        ResourceType.PATIENT,
                        updatedPatient.getId(),
                        """
                        {
                        "patientCode":"%s",
                        "consentAgreed":%s,
                        "consentVersion":"%s",
                        "consentWithdrawn":%s,
                        "consentWithdrawnReason":"%s",
                        "nonMedicalUseRestricted":%s,
                        "versionNumber":%d,
                        "historyStatus":"%s",
                        "scopes":"%s"
                        }
                        """.formatted(
                                updatedPatient.getPatientCode(),
                                updatedPatient.isConsentAgreed(),
                                updatedPatient.getConsentVersion(),
                                updatedPatient.isConsentWithdrawn(),
                                updatedPatient.getConsentWithdrawnReason() != null ? updatedPatient.getConsentWithdrawnReason() : "",
                                updatedPatient.isNonMedicalUseRestricted(),
                                nextVersionNumber,
                                historyStatus.name(),
                                ConsentScope.toCommaSeparated(effectiveScopes)
                        ),
                        null,
                        now
                )
        );

        return patientResultMapper.toResult(updatedPatient);
    }
}

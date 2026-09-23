package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordRetentionPolicy;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.PatientConsentRecord;
import com.benhsoan.domain.patient.PatientConsentVersion;
import com.benhsoan.domain.patient.enums.ConsentHistoryStatus;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.exception.PatientConsentAccessDeniedException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.RequestPatientDataErasureCommand;
import com.benhsoan.port.dto.result.patient.DataErasureResult;
import com.benhsoan.port.inbound.patient.RequestPatientDataErasureUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientConsentHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Service tiếp nhận và xử lý yêu cầu xóa dữ liệu của người bệnh theo AC-03 & QTN-19.
 * Hệ thống từ chối xóa hồ sơ bệnh án chuyên môn do quy định lưu trữ tối thiểu 10 năm,
 * đồng thời rút lại toàn bộ sự đồng ý cho các hoạt động ngoài khám chữa bệnh.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RequestPatientDataErasureService implements RequestPatientDataErasureUseCase {

    private final PatientRepository patientRepository;
    private final PatientConsentHistoryRepository patientConsentHistoryRepository;
    private final MedicalRecordRetentionPolicy medicalRecordRetentionPolicy;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final PatientConsentErasureAuditWriter erasureAuditWriter;
    private final PatientChangeLogRepository patientChangeLogRepository;
    private final PatientChangeDetailBuilder changeDetailBuilder;
    private final PatientAccessGuard patientAccessGuard;

    @Override
    public DataErasureResult requestErasure(UUID patientId, RequestPatientDataErasureCommand command) {
        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort != null ? clockPort.now() : Instant.now();

        // 1. Phân quyền
        boolean isStaff = currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE");
        if (!isStaff) {
            try {
                patientAccessGuard.requirePatientOwnership(patientId);
            } catch (AccessDeniedException e) {
                throw new PatientConsentAccessDeniedException();
            }
        }

        // 2. Tìm hồ sơ bệnh nhân
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Không tìm thấy thông tin bệnh nhân: " + patientId));

        patient.validateCanBeUpdated();

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

        // 3. Áp dụng QTN-19: Bảo toàn hồ sơ bệnh án, không xóa các bản ghi y tế
        int retentionYears = medicalRecordRetentionPolicy.retentionYears();
        String rejectionReason = (command != null && command.reason() != null && !command.reason().isBlank())
                ? command.reason()
                : "Yêu cầu xóa toàn bộ dữ liệu cá nhân (Áp dụng QTN-19: Bảo lưu hồ sơ bệnh án)";

        // Cập nhật trạng thái consent sang WITHDRAWN và hạn chế dùng ngoài y tế
        patient.withdrawConsent(rejectionReason, now);
        Patient updatedPatient = patientRepository.save(patient);

        // 4. Lưu bản ghi lịch sử mới (AC-02, AC-03)
        int nextVersionNumber = patientConsentHistoryRepository.getNextVersionNumber(patientId);
        String versionCode = patient.getConsentVersion() != null ? patient.getConsentVersion() : PatientConsentVersion.current();

        PatientConsentRecord historyRecord = PatientConsentRecord.create(
                patientId,
                nextVersionNumber,
                versionCode,
                ConsentHistoryStatus.WITHDRAWN,
                Collections.emptySet(),
                patient.isConsentAgreed(),
                patient.getConsentAgreedAt(),
                true,
                now,
                rejectionReason,
                true,
                patient.getConsentSignerName(),
                currentUserId,
                now
        );
        patientConsentHistoryRepository.save(historyRecord);

        // 5. Ghi nhật ký thay đổi và AuditLog
        String detail = changeDetailBuilder.forUpdate(oldPatient, updatedPatient);
        PatientChangeLog log = PatientChangeLog.create(
                updatedPatient.getId(),
                currentUserId,
                PatientChangeAction.UPDATE,
                detail
        );
        patientChangeLogRepository.save(log);

        erasureAuditWriter.writeErasureRefusal(
                currentUserId,
                updatedPatient.getId(),
                updatedPatient.getPatientCode(),
                retentionYears,
                rejectionReason,
                now
        );

        String message = "Hồ sơ bệnh án được lưu trữ tối thiểu %d năm theo Luật Khám bệnh, chữa bệnh (QTN-19) và không thể xóa trước hạn. Hệ thống đã thu hồi sự đồng ý đối với các mục đích xử lý ngoài khám chữa bệnh."
                .formatted(retentionYears);

        return DataErasureResult.builder()
                .patientId(patientId)
                .consentWithdrawn(true)
                .nonMedicalUseRestricted(true)
                .medicalRecordsRetained(true)
                .retentionYears(retentionYears)
                .message(message)
                .build();
    }
}

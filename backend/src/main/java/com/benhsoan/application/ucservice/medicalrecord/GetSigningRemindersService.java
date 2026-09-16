package com.benhsoan.application.ucservice.medicalrecord;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordSigningReminder;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.SigningReminderResult;
import com.benhsoan.port.inbound.medicalrecord.GetSigningRemindersUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordSigningReminderRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSigningRemindersService implements GetSigningRemindersUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordSigningReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final CurrentUserPort currentUserPort;
    private final MedicalRecordAuthorizationAuditService authorizationAuditService;

    @Override
    public List<SigningReminderResult> getReminders(UUID medicalRecordId) {
        if (medicalRecordId == null) {
            throw new ValidationException("Medical record id is required.");
        }

        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));

        if (currentUserPort.hasRole("DOCTOR")
                && !currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("MANAGER")) {
            UUID currentDoctorId = currentUserPort.getCurrentUserId();
            Visit visit = visitRepository.findById(record.getVisitId()).orElse(null);
            if (visit == null || !visit.getDoctorId().equals(currentDoctorId)) {
                authorizationAuditService.recordTemplateAccessDenied(
                        currentDoctorId,
                        medicalRecordId,
                        "Doctor attempted to view signing reminders of another doctor's medical record"
                );
                throw new MedicalRecordAccessDeniedException();
            }
        }

        List<MedicalRecordSigningReminder> reminders = reminderRepository.findByMedicalRecordId(medicalRecordId);
        if (reminders.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = reminders.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getDoctorId(), r.getRemindedBy()))
                .distinct()
                .toList();
        Map<UUID, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (existing, replacement) -> existing));

        return reminders.stream()
                .map(r -> new SigningReminderResult(
                        r.getId(),
                        r.getMedicalRecordId(),
                        r.getDoctorId(),
                        userNames.getOrDefault(r.getDoctorId(), "Bác sĩ phụ trách"),
                        r.getRemindedBy(),
                        userNames.getOrDefault(r.getRemindedBy(), "Người dùng hệ thống"),
                        r.getRemindedAt(),
                        r.getOverdueHours(),
                        r.getChannel(),
                        r.getNotes(),
                        r.getStatus()
                ))
                .toList();
    }
}

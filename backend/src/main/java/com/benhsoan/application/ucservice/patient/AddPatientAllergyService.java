package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.patient.PatientAllergyChangeLog;
import com.benhsoan.domain.patient.exception.PatientAllergyAlreadyExistsException;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.patient.AddPatientAllergyCommand;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;
import com.benhsoan.port.inbound.patient.AddPatientAllergyUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AddPatientAllergyService implements AddPatientAllergyUseCase {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientAllergyChangeLogRepository changeLogRepository;
    private final VisitRepository visitRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final PatientAllResultMapper resultMapper;
    private final PatientAllergyChangeDetailBuilder changeDetailBuilder;

    @Override
    public PatientAllergyResult addAllergy(AddPatientAllergyCommand command) {
        if (command == null || command.patientId() == null) {
            throw new ValidationException("Patient ID is required.");
        }
        if (command.allergenName() == null || command.allergenName().isBlank()) {
            throw new ValidationException("Tên hoạt chất hoặc nhóm thuốc không được để trống.");
        }
        if (command.severity() == null) {
            throw new ValidationException("Mức độ phản ứng không được để trống.");
        }

        Patient patient = patientRepository.findById(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));

        if (!patient.isActive()) {
            throw new PatientInactiveException();
        }

        if (command.visitId() != null) {
            Visit visit = visitRepository.findById(command.visitId())
                    .orElseThrow(() -> new VisitNotFoundException(command.visitId()));
            if (!visit.getPatientId().equals(command.patientId())) {
                throw new ValidationException("Lượt khám không thuộc về bệnh nhân này.");
            }
            if (!visit.isActive()) {
                throw new ValidationException("Lượt khám đã kết thúc hoặc không còn hiệu lực.");
            }
        }

        String normalized = PatientAllergy.normalizeAllergenName(command.allergenName());
        if (patientAllergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(command.patientId(), normalized)) {
            throw new PatientAllergyAlreadyExistsException(command.allergenName());
        }

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        PatientAllergy allergy = PatientAllergy.create(
                command.patientId(),
                command.allergenType(),
                command.allergenName(),
                command.severity(),
                command.reaction(),
                command.notes(),
                currentUserId,
                now
        );

        PatientAllergy saved;
        try {
            saved = patientAllergyRepository.save(allergy);
        } catch (DataIntegrityViolationException e) {
            throw new PatientAllergyAlreadyExistsException(command.allergenName());
        }

        String afterData = changeDetailBuilder.buildSnapshot(saved);
        String reason = command.visitId() != null
                ? "Ghi nhận dị ứng trong lượt khám: " + command.visitId()
                : "Ghi nhận dị ứng ban đầu";

        PatientAllergyChangeLog changeLog = PatientAllergyChangeLog.create(
                saved.getId(),
                command.patientId(),
                "CREATE",
                null,
                afterData,
                reason,
                currentUserId,
                now
        );
        changeLogRepository.save(changeLog);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.CREATE,
                ResourceType.PATIENT_ALLERGY,
                saved.getId(),
                afterData,
                null,
                now
        ));

        return resultMapper.toResult(saved);
    }
}

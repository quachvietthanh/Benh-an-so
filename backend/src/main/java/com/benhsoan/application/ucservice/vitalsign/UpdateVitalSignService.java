package com.benhsoan.application.ucservice.vitalsign;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.exception.VisitInvalidStatusException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.domain.vitalsign.exception.VitalSignNotFoundException;
import com.benhsoan.port.dto.command.vitalsign.UpdateVitalSignCommand;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;
import com.benhsoan.port.inbound.vitalsign.UpdateVitalSignUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.vitalsign.VitalSignRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateVitalSignService implements UpdateVitalSignUseCase {

    private final VitalSignRepository vitalSignRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final VitalSignAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final AuditLogRepository auditLogRepository;
    private final VitalSignResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public VitalSignResult update(UUID id, UpdateVitalSignCommand command) {
        UUID actorId = authorizationService.requireWriteAccess();

        VitalSign vitalSign = vitalSignRepository.findById(id)
                .orElseThrow(() -> new VitalSignNotFoundException(id));

        Visit visit = visitRepository.findById(vitalSign.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(vitalSign.getVisitId()));

        authorizationService.requireVisitDoctorAccess(visit.getDoctorId());

        if (visit.getStatus() != VisitStatus.IN_PROGRESS && visit.getStatus() != VisitStatus.WAITING_FOR_RESULT) {
            throw new VisitInvalidStatusException("Chỉ được cập nhật chỉ số sinh tồn khi lượt khám đang diễn ra.");
        }

        var medicalRecordOpt = medicalRecordRepository.findByVisitId(visit.getId());
        if (medicalRecordOpt.isPresent()) {
            MedicalRecord record = medicalRecordOpt.get();
            record.ensureEditable(); // QTN-07
            if (vitalSign.getMedicalRecordId() == null) {
                vitalSign.attachMedicalRecord(record.getId());
            }
        } else if (vitalSign.getMedicalRecordId() != null) {
            medicalRecordRepository.findById(vitalSign.getMedicalRecordId())
                    .ifPresent(MedicalRecord::ensureEditable); // QTN-07
        }

        Instant now = clockPort.now();

        vitalSign.update(
                command.pulse(),
                command.bloodPressureSystolic(),
                command.bloodPressureDiastolic(),
                command.temperature(),
                command.respiratoryRate(),
                command.weight(),
                command.height(),
                command.spo2(),
                command.note(),
                actorId,
                now
        );

        VitalSign saved = vitalSignRepository.save(vitalSign);

        // QTN-02: Ghi nhật ký truy cập bệnh án
        accessAuditService.recordRecordAccess(
                vitalSign.getPatientId(),
                vitalSign.getVisitId(),
                vitalSign.getMedicalRecordId(),
                actorId,
                MedicalRecordAccessAction.UPDATE,
                "Chỉ số sinh tồn được cập nhật: " + saved.getId(),
                now
        );

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.VITAL_SIGN,
                saved.getId(),
                "Cập nhật chỉ số sinh tồn cho lượt khám " + visit.getVisitCode(),
                null
        ));

        return resultMapper.toResult(saved);
    }
}

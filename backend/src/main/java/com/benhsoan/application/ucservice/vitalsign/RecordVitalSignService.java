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
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.exception.VisitInvalidStatusException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.port.dto.command.vitalsign.RecordVitalSignCommand;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;
import com.benhsoan.port.inbound.vitalsign.RecordVitalSignUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.vitalsign.VitalSignRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordVitalSignService implements RecordVitalSignUseCase {

    private final VitalSignRepository vitalSignRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final VitalSignAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final AuditLogRepository auditLogRepository;
    private final VitalSignResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public VitalSignResult record(RecordVitalSignCommand command) {
        UUID actorId = authorizationService.requireWriteAccess();

        Visit visit = visitRepository.findById(command.visitId())
                .orElseThrow(() -> new VisitNotFoundException(command.visitId()));

        authorizationService.requireVisitDoctorAccess(visit.getDoctorId());

        if (visit.getStatus() != VisitStatus.IN_PROGRESS && visit.getStatus() != VisitStatus.WAITING_FOR_RESULT) {
            throw new VisitInvalidStatusException("Chỉ được ghi nhận chỉ số sinh tồn khi lượt khám đang diễn ra.");
        }

        MedicalRecord record = medicalRecordRepository.findByVisitId(visit.getId())
                .orElseThrow(() -> new MedicalRecordNotFoundException(visit.getId()));
        record.ensureEditable(); // QTN-07
        UUID medicalRecordId = record.getId();

        Instant now = clockPort.now();

        VitalSign vitalSign = VitalSign.create(
                visit.getId(),
                visit.getPatientId(),
                medicalRecordId,
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
                visit.getPatientId(),
                visit.getId(),
                medicalRecordId,
                actorId,
                MedicalRecordAccessAction.CREATE,
                "Chỉ số sinh tồn được ghi nhận: " + saved.getId(),
                now
        );

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CREATE,
                ResourceType.VITAL_SIGN,
                saved.getId(),
                "Ghi nhận chỉ số sinh tồn cho lượt khám " + visit.getVisitCode(),
                null
        ));

        return resultMapper.toResult(saved);
    }
}

package com.benhsoan.application.ucservice.vitalsign;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;
import com.benhsoan.port.inbound.vitalsign.GetPatientVitalSignHistoryUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.vitalsign.VitalSignRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientVitalSignHistoryService implements GetPatientVitalSignHistoryUseCase {

    private final VitalSignRepository vitalSignRepository;
    private final PatientRepository patientRepository;
    private final VitalSignAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final VitalSignResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public List<VitalSignResult> getHistory(UUID patientId) {
        UUID actorId = authorizationService.requireReadAccess();

        patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        // QTN-02: Ghi nhật ký truy cập lịch sử hồ sơ bệnh án
        accessAuditService.recordHistoryView(patientId, actorId, clockPort.now());

        List<VitalSign> history = vitalSignRepository.findHistoryByPatientId(patientId);
        return resultMapper.toResults(history);
    }
}

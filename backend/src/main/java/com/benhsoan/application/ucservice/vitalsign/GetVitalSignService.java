package com.benhsoan.application.ucservice.vitalsign;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.domain.vitalsign.exception.VitalSignNotFoundException;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;
import com.benhsoan.port.inbound.vitalsign.GetVitalSignUseCase;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.vitalsign.VitalSignRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVitalSignService implements GetVitalSignUseCase {

    private final VitalSignRepository vitalSignRepository;
    private final VisitRepository visitRepository;
    private final VitalSignAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final VitalSignResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public VitalSignResult getById(UUID id) {
        VitalSign vitalSign = vitalSignRepository.findById(id)
                .orElseThrow(() -> new VitalSignNotFoundException(id));

        Visit visit = visitRepository.findById(vitalSign.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(vitalSign.getVisitId()));

        UUID actorId = authorizationService.requireVisitReadAccess(visit.getDoctorId(), visit.getId());

        accessAuditService.recordRecordView(
                vitalSign.getPatientId(),
                vitalSign.getVisitId(),
                vitalSign.getMedicalRecordId(),
                actorId,
                clockPort.now()
        );

        return resultMapper.toResult(vitalSign);
    }

    @Override
    public Optional<VitalSignResult> getLatestByVisitId(UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));

        UUID actorId = authorizationService.requireVisitReadAccess(visit.getDoctorId(), visit.getId());

        Optional<VitalSign> vitalSignOpt = vitalSignRepository.findLatestByVisitId(visitId);
        vitalSignOpt.ifPresent(vs -> accessAuditService.recordRecordView(
                vs.getPatientId(),
                vs.getVisitId(),
                vs.getMedicalRecordId(),
                actorId,
                clockPort.now()
        ));

        return vitalSignOpt.map(resultMapper::toResult);
    }

    @Override
    public List<VitalSignResult> getByVisitId(UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));

        UUID actorId = authorizationService.requireVisitReadAccess(visit.getDoctorId(), visit.getId());

        List<VitalSign> list = vitalSignRepository.findByVisitId(visitId);
        if (!list.isEmpty()) {
            VitalSign first = list.get(0);
            accessAuditService.recordRecordView(
                    first.getPatientId(),
                    first.getVisitId(),
                    first.getMedicalRecordId(),
                    actorId,
                    clockPort.now()
            );
        }

        return resultMapper.toResults(list);
    }
}

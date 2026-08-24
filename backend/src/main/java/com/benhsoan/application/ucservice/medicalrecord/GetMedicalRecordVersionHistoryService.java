package com.benhsoan.application.ucservice.medicalrecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.MedicalRecordVersion;
import com.benhsoan.port.dto.result.MedicalRecordVersionHistoryResult;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordVersionHistoryUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAmendmentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Query service for medical record version/amendment history (NCL-11-CN-003).
 * Enforces read authorization (TC-03), writes a denial audit on refusal, and
 * records a medical record access log on success (TC-04).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMedicalRecordVersionHistoryService implements GetMedicalRecordVersionHistoryUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordAmendmentRepository amendmentRepository;
    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordVersionHistoryAuditWriter denialAuditWriter;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordVersionHistoryResult getVersionHistory(UUID medicalRecordId) {
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));

        UUID userId = authorizeVersionHistoryRead(record);

        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));

        accessAuditService.recordRecordAccess(
                visit.getPatientId(), visit.getId(), record.getId(), userId,
                MedicalRecordAccessAction.VIEW, "Medical record version history viewed", clockPort.now());

        List<MedicalRecordAmendment> amendments = amendmentRepository.findByMedicalRecordId(record.getId()).stream()
                .sorted(Comparator.comparing(MedicalRecordAmendment::getAmendedAt))
                .toList();

        Map<UUID, String> names = resolveNames(record, amendments);

        MedicalRecordVersion originalVersion = new MedicalRecordVersion(
                1, names.get(record.getCreatedBy()), record.getCreatedAt(), null, null);

        List<MedicalRecordVersion> amendmentVersions = new ArrayList<>();
        for (int index = 0; index < amendments.size(); index++) {
            MedicalRecordAmendment amendment = amendments.get(index);
            amendmentVersions.add(new MedicalRecordVersion(
                    index + 2,
                    names.get(amendment.getAmendedBy()),
                    amendment.getAmendedAt(),
                    amendment.getReason(),
                    amendment.getContent()));
        }

        return new MedicalRecordVersionHistoryResult(amendments.isEmpty(), originalVersion, amendmentVersions);
    }

    private UUID authorizeVersionHistoryRead(MedicalRecord record) {
        try {
            return authorizationService.requireReadAccess();
        } catch (MedicalRecordAccessDeniedException denied) {
            denialAuditWriter.writeDenied(currentUserPort.getCurrentUserId(), record.getId(),
                    "Medical record version history access denied.", clockPort.now());
            throw denied;
        }
    }

    private Map<UUID, String> resolveNames(MedicalRecord record, List<MedicalRecordAmendment> amendments) {
        List<UUID> ids = new ArrayList<>();
        ids.add(record.getCreatedBy());
        amendments.forEach(amendment -> ids.add(amendment.getAmendedBy()));
        return userRepository.findAllById(ids.stream().filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }
}

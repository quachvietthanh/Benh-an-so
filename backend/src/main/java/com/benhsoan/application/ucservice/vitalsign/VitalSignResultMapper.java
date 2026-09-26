package com.benhsoan.application.ucservice.vitalsign;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;

@Component
public class VitalSignResultMapper {

    public VitalSignResult toResult(VitalSign domain) {
        if (domain == null) {
            return null;
        }

        return new VitalSignResult(
                domain.getId(),
                domain.getVisitId(),
                domain.getPatientId(),
                domain.getMedicalRecordId(),
                domain.getPulse(),
                domain.getBloodPressureSystolic(),
                domain.getBloodPressureDiastolic(),
                domain.getTemperature(),
                domain.getRespiratoryRate(),
                domain.getWeight(),
                domain.getHeight(),
                domain.getBmi(),
                domain.getSpo2(),
                domain.isAbnormal(),
                domain.getAbnormalFlags(),
                domain.getNote(),
                domain.getRecordedBy(),
                domain.getRecordedAt(),
                domain.getUpdatedBy(),
                domain.getUpdatedAt()
        );
    }

    public List<VitalSignResult> toResults(List<VitalSign> domains) {
        if (domains == null) {
            return List.of();
        }
        return domains.stream()
                .map(this::toResult)
                .collect(Collectors.toList());
    }
}

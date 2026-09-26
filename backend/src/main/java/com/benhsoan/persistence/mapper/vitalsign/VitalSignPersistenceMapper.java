package com.benhsoan.persistence.mapper.vitalsign;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.domain.vitalsign.enums.VitalSignAbnormalFlag;
import com.benhsoan.persistence.entity.vitalsign.VitalSignEntity;

@Component
public class VitalSignPersistenceMapper {

    public VitalSignEntity toEntity(VitalSign domain) {
        if (domain == null) {
            return null;
        }

        String flagsString = domain.getAbnormalFlags() == null || domain.getAbnormalFlags().isEmpty()
                ? null
                : domain.getAbnormalFlags().stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(","));

        return VitalSignEntity.builder()
                .id(domain.getId())
                .visitId(domain.getVisitId())
                .patientId(domain.getPatientId())
                .medicalRecordId(domain.getMedicalRecordId())
                .pulse(domain.getPulse())
                .bloodPressureSystolic(domain.getBloodPressureSystolic())
                .bloodPressureDiastolic(domain.getBloodPressureDiastolic())
                .temperature(domain.getTemperature())
                .respiratoryRate(domain.getRespiratoryRate())
                .weight(domain.getWeight())
                .height(domain.getHeight())
                .bmi(domain.getBmi())
                .spo2(domain.getSpo2())
                .abnormal(domain.isAbnormal())
                .abnormalFlags(flagsString)
                .note(domain.getNote())
                .recordedBy(domain.getRecordedBy())
                .recordedAt(domain.getRecordedAt())
                .updatedBy(domain.getUpdatedBy())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public VitalSign toDomain(VitalSignEntity entity) {
        if (entity == null) {
            return null;
        }

        List<VitalSignAbnormalFlag> flags = parseFlags(entity.getAbnormalFlags());

        return VitalSign.restore(
                entity.getId(),
                entity.getVisitId(),
                entity.getPatientId(),
                entity.getMedicalRecordId(),
                entity.getPulse(),
                entity.getBloodPressureSystolic(),
                entity.getBloodPressureDiastolic(),
                entity.getTemperature(),
                entity.getRespiratoryRate(),
                entity.getWeight(),
                entity.getHeight(),
                entity.getBmi(),
                entity.getSpo2(),
                entity.isAbnormal(),
                flags,
                entity.getNote(),
                entity.getRecordedBy(),
                entity.getRecordedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }

    private List<VitalSignAbnormalFlag> parseFlags(String flagsString) {
        if (flagsString == null || flagsString.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(flagsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return VitalSignAbnormalFlag.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

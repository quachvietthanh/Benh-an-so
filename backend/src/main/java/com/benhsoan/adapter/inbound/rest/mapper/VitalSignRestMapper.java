package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.vitalsign.RecordVitalSignRequest;
import com.benhsoan.adapter.inbound.rest.request.vitalsign.UpdateVitalSignRequest;
import com.benhsoan.adapter.inbound.rest.response.vitalsign.VitalSignResponse;
import com.benhsoan.port.dto.command.vitalsign.RecordVitalSignCommand;
import com.benhsoan.port.dto.command.vitalsign.UpdateVitalSignCommand;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;

@Component
public class VitalSignRestMapper {

    public RecordVitalSignCommand toCommand(RecordVitalSignRequest request) {
        if (request == null) {
            return null;
        }
        return new RecordVitalSignCommand(
                request.getVisitId(),
                request.getPulse(),
                request.getBloodPressureSystolic(),
                request.getBloodPressureDiastolic(),
                request.getTemperature(),
                request.getRespiratoryRate(),
                request.getWeight(),
                request.getHeight(),
                request.getSpo2(),
                request.getNote()
        );
    }

    public UpdateVitalSignCommand toCommand(UpdateVitalSignRequest request) {
        if (request == null) {
            return null;
        }
        return new UpdateVitalSignCommand(
                request.getPulse(),
                request.getBloodPressureSystolic(),
                request.getBloodPressureDiastolic(),
                request.getTemperature(),
                request.getRespiratoryRate(),
                request.getWeight(),
                request.getHeight(),
                request.getSpo2(),
                request.getNote()
        );
    }

    public VitalSignResponse toResponse(VitalSignResult result) {
        if (result == null) {
            return null;
        }
        return VitalSignResponse.builder()
                .id(result.id())
                .visitId(result.visitId())
                .patientId(result.patientId())
                .medicalRecordId(result.medicalRecordId())
                .pulse(result.pulse())
                .bloodPressureSystolic(result.bloodPressureSystolic())
                .bloodPressureDiastolic(result.bloodPressureDiastolic())
                .temperature(result.temperature())
                .respiratoryRate(result.respiratoryRate())
                .weight(result.weight())
                .height(result.height())
                .bmi(result.bmi())
                .spo2(result.spo2())
                .abnormal(result.abnormal())
                .abnormalFlags(result.abnormalFlags())
                .note(result.note())
                .recordedBy(result.recordedBy())
                .recordedAt(result.recordedAt())
                .updatedBy(result.updatedBy())
                .updatedAt(result.updatedAt())
                .build();
    }

    public List<VitalSignResponse> toResponses(List<VitalSignResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}

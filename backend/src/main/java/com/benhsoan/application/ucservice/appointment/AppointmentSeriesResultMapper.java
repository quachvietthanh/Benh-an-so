package com.benhsoan.application.ucservice.appointment;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.AppointmentSeries;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult;

@Component
public class AppointmentSeriesResultMapper {

    public AppointmentSeriesResult toResult(AppointmentSeries series, List<AppointmentResult> appointments) {
        if (series == null) {
            return null;
        }

        return AppointmentSeriesResult.builder()
                .id(series.getId())
                .seriesCode(series.getSeriesCode())
                .patientId(series.getPatientId())
                .doctorId(series.getDoctorId())
                .medicalRecordId(series.getMedicalRecordId())
                .totalSessions(series.getTotalSessions())
                .intervalDays(series.getIntervalDays())
                .title(series.getTitle())
                .notes(series.getNotes())
                .status(series.getStatus())
                .createdBy(series.getCreatedBy())
                .createdAt(series.getCreatedAt())
                .updatedAt(series.getUpdatedAt())
                .appointments(appointments != null ? appointments : List.of())
                .build();
    }
}

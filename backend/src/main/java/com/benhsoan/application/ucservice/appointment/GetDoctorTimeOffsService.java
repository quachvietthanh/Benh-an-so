package com.benhsoan.application.ucservice.appointment;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.port.dto.query.appointment.GetDoctorTimeOffsQuery;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.inbound.appointment.GetDoctorTimeOffsUseCase;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDoctorTimeOffsService implements GetDoctorTimeOffsUseCase {

    private final DoctorTimeOffRepository doctorTimeOffRepository;

    @Override
    public List<DoctorTimeOffResult> getTimeOffs(GetDoctorTimeOffsQuery query) {
        List<DoctorTimeOff> list = doctorTimeOffRepository.search(
                query.doctorId(),
                query.status(),
                query.fromTime(),
                query.toTime()
        );

        return list.stream()
                .map(this::toResult)
                .toList();
    }

    private DoctorTimeOffResult toResult(DoctorTimeOff timeOff) {
        return new DoctorTimeOffResult(
                timeOff.getId(),
                timeOff.getDoctorId(),
                timeOff.getStartTime(),
                timeOff.getEndTime(),
                timeOff.getReason(),
                timeOff.getStatus(),
                timeOff.getCreatedBy(),
                timeOff.getCreatedAt(),
                timeOff.getUpdatedAt(),
                List.of()
        );
    }
}

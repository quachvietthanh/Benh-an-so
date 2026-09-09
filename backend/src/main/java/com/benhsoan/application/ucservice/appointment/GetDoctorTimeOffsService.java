package com.benhsoan.application.ucservice.appointment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.inbound.appointment.GetDoctorTimeOffsUseCase;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDoctorTimeOffsService implements GetDoctorTimeOffsUseCase {

    private final DoctorTimeOffRepository doctorTimeOffRepository;
    private final UserRepository userRepository;

    @Override
    public List<DoctorTimeOffResult> getTimeOffs(UUID doctorId) {
        userRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));

        return doctorTimeOffRepository.findByDoctorId(doctorId).stream()
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
                List.of()
        );
    }
}

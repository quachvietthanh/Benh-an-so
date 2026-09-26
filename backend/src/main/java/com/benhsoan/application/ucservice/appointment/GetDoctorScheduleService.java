package com.benhsoan.application.ucservice.appointment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;
import com.benhsoan.port.inbound.appointment.GetDoctorScheduleUseCase;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDoctorScheduleService implements GetDoctorScheduleUseCase {

    private final DoctorWeeklyScheduleRepository weeklyScheduleRepository;
    private final UserRepository userRepository;

    @Override
    public List<DoctorWeeklyScheduleResult> getWeeklySchedule(UUID doctorId) {
        userRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));

        return weeklyScheduleRepository.findByDoctorId(doctorId).stream()
                .map(this::toResult)
                .toList();
    }

    private DoctorWeeklyScheduleResult toResult(DoctorWeeklySchedule ws) {
        return new DoctorWeeklyScheduleResult(
                ws.getId(),
                ws.getDoctorId(),
                ws.getDayOfWeek(),
                ws.getStartTime(),
                ws.getEndTime(),
                ws.isActive()
        );
    }
}

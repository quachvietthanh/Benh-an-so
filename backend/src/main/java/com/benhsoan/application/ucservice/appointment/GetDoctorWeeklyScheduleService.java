package com.benhsoan.application.ucservice.appointment;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.port.dto.query.appointment.GetDoctorWeeklyScheduleQuery;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleItemResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;
import com.benhsoan.port.inbound.appointment.GetDoctorWeeklyScheduleUseCase;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDoctorWeeklyScheduleService implements GetDoctorWeeklyScheduleUseCase {

    private final DoctorWeeklyScheduleRepository doctorWeeklyScheduleRepository;

    @Override
    public DoctorWeeklyScheduleResult getWeeklySchedule(GetDoctorWeeklyScheduleQuery query) {
        List<DoctorWeeklyScheduleItemResult> items = doctorWeeklyScheduleRepository.findByDoctorId(query.doctorId()).stream()
                .sorted(Comparator.comparing(DoctorWeeklySchedule::getDayOfWeek)
                        .thenComparing(DoctorWeeklySchedule::getStartTime))
                .map(s -> new DoctorWeeklyScheduleItemResult(
                        s.getId(),
                        s.getDoctorId(),
                        s.getDayOfWeek(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.isActive()
                ))
                .toList();

        return new DoctorWeeklyScheduleResult(query.doctorId(), items);
    }
}

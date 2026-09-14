package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.GetUnconfirmedAppointmentsUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-03-CN-008 TC-04: Returns unconfirmed appointments for a date
 * ordered by start time ascending for receptionists to call and remind.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUnconfirmedAppointmentsService implements GetUnconfirmedAppointmentsUseCase {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public Page<AppointmentResult> getUnconfirmed(LocalDate date, Pageable pageable) {
        Instant now = clockPort.now();
        LocalDate today = now.atZone(CLINIC_ZONE).toLocalDate();
        LocalDate targetDate = (date != null) ? date : today;

        if (targetDate.isBefore(today)) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Instant fromTime = targetDate.isEqual(today)
                ? now
                : targetDate.atStartOfDay(CLINIC_ZONE).toInstant().minusMillis(1);
        Instant endOfDay = targetDate.atTime(LocalTime.MAX).atZone(CLINIC_ZONE).toInstant();

        Page<Appointment> appointments = appointmentRepository.findUnconfirmed(fromTime, endOfDay, pageable);
        return appointments.map(a -> resultMapper.toResult(a, List.of()));
    }
}

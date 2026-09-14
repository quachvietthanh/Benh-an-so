package com.benhsoan.port.inbound.appointment;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.AppointmentResult;

public interface GetUnconfirmedAppointmentsUseCase {

    Page<AppointmentResult> getUnconfirmed(LocalDate date, Pageable pageable);

}

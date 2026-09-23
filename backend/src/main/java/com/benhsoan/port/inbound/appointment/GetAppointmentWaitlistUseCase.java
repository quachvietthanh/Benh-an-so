package com.benhsoan.port.inbound.appointment;

import java.util.List;

import com.benhsoan.port.dto.query.appointment.GetAppointmentWaitlistQuery;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;

public interface GetAppointmentWaitlistUseCase {
    List<AppointmentWaitlistResult> getWaitlist(GetAppointmentWaitlistQuery query);
}

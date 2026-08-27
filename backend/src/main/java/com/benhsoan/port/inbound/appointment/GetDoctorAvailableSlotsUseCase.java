package com.benhsoan.port.inbound.appointment;

import java.util.List;

import com.benhsoan.port.dto.query.appointment.GetDoctorAvailableSlotsQuery;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;

public interface GetDoctorAvailableSlotsUseCase {

    List<DoctorAvailableSlotResult> getAvailableSlots(GetDoctorAvailableSlotsQuery query);

}

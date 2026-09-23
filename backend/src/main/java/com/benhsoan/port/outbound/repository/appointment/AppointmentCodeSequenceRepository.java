package com.benhsoan.port.outbound.repository.appointment;

public interface AppointmentCodeSequenceRepository {

    long reserveNextValue(String prefix);

    long reserveNextValues(String prefix, int count);
}

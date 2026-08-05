package com.benhsoan.port.outbound.repository.prescription;

public interface PrescriptionCodeSequenceRepository {

    long reserveNextValue(String prefix);
}

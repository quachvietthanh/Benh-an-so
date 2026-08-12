package com.benhsoan.port.outbound.repository.billing;

public interface InvoiceCodeSequenceRepository {

    long reserveNextValue(String prefix);
}

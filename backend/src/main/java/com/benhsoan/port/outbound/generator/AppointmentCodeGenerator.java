package com.benhsoan.port.outbound.generator;

import java.util.List;

public interface AppointmentCodeGenerator {

    String generate();

    List<String> generateBatch(int count);
}
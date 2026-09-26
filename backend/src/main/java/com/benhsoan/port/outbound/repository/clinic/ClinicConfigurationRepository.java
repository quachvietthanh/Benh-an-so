package com.benhsoan.port.outbound.repository.clinic;

import java.util.Optional;

import com.benhsoan.domain.clinic.ClinicConfiguration;

public interface ClinicConfigurationRepository {

    Optional<ClinicConfiguration> find();

    ClinicConfiguration save(ClinicConfiguration configuration);
}

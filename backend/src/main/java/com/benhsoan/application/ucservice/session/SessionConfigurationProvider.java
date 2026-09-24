package com.benhsoan.application.ucservice.session;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.session.SessionSettings;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SessionConfigurationProvider {

    private final ClinicConfigurationRepository clinicConfigurationRepository;

    public SessionSettings currentSettings() {
        return clinicConfigurationRepository.find()
                .map(this::toSettings)
                .orElseGet(this::defaultSettings);
    }

    private SessionSettings toSettings(ClinicConfiguration configuration) {
        return new SessionSettings(
                Duration.ofMinutes(configuration.getSessionTimeoutMinutes()),
                Duration.ofMinutes(configuration.getSessionWarningMinutes())
        );
    }

    private SessionSettings defaultSettings() {
        return new SessionSettings(
                Duration.ofMinutes(ClinicConfiguration.DEFAULT_SESSION_TIMEOUT_MINUTES),
                Duration.ofMinutes(ClinicConfiguration.DEFAULT_SESSION_WARNING_MINUTES)
        );
    }
}
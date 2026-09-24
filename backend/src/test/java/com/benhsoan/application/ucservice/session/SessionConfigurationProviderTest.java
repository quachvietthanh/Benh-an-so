package com.benhsoan.application.ucservice.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.session.SessionSettings;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;

@ExtendWith(MockitoExtension.class)
class SessionConfigurationProviderTest {

    @Mock
    private ClinicConfigurationRepository clinicConfigurationRepository;

    @Test
    void returnsDefaultsWhenNoConfigurationExists() {
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
        SessionConfigurationProvider provider = new SessionConfigurationProvider(clinicConfigurationRepository);

        SessionSettings settings = provider.currentSettings();

        assertEquals(Duration.ofMinutes(30), settings.inactivityTimeout());
        assertEquals(Duration.ofMinutes(5), settings.warningThreshold());
    }

    @Test
    void mapsConfiguredValues() {
        ClinicConfiguration config = ClinicConfiguration.create(
                "Phong kham Benh So An", "Thai Nguyen", "0345678910",
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                10, 24, 15, 3, Instant.parse("2026-09-24T08:00:00Z"));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(config));
        SessionConfigurationProvider provider = new SessionConfigurationProvider(clinicConfigurationRepository);

        SessionSettings settings = provider.currentSettings();

        assertEquals(Duration.ofMinutes(15), settings.inactivityTimeout());
        assertEquals(Duration.ofMinutes(3), settings.warningThreshold());
    }
}
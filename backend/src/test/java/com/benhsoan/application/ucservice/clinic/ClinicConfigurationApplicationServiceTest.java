package com.benhsoan.application.ucservice.clinic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinic.UpdateClinicConfigurationCommand;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ClinicConfigurationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T01:00:00Z");

    @Mock
    private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock
    private ClockPort clockPort;

    private GetClinicConfigurationService getService;
    private UpdateClinicConfigurationService updateService;

    @BeforeEach
    void setUp() {
        ClinicConfigurationResultMapper resultMapper = new ClinicConfigurationResultMapper();
        getService = new GetClinicConfigurationService(clinicConfigurationRepository, resultMapper);
        updateService = new UpdateClinicConfigurationService(
                clinicConfigurationRepository,
                clockPort,
                resultMapper
        );
    }

    @Test
    void getReturnsAnEmptyResultWhenNoConfigurationExists() {
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());

        var result = getService.get();

        assertNull(result.clinicName());
        assertNull(result.openingTime());
        assertNull(result.closingTime());
    }

    @Test
    void updateCreatesTheSingletonWhenItDoesNotExist() {
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);
        when(clinicConfigurationRepository.save(any(ClinicConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = updateService.update(command("Phong kham Benh So An", LocalTime.of(8, 0), LocalTime.of(17, 0)));

        assertEquals("Phong kham Benh So An", result.clinicName());
        ArgumentCaptor<ClinicConfiguration> configurationCaptor = ArgumentCaptor.forClass(ClinicConfiguration.class);
        verify(clinicConfigurationRepository).save(configurationCaptor.capture());
        assertEquals(ClinicConfiguration.SINGLETON_ID, configurationCaptor.getValue().getId());
        assertEquals(NOW, configurationCaptor.getValue().getCreatedAt());
    }

    @Test
    void updateChangesTheExistingSingleton() {
        ClinicConfiguration existing = ClinicConfiguration.create(
                "Ten cu", null, null, LocalTime.of(8, 0), LocalTime.of(17, 0),
                Instant.parse("2026-08-01T01:00:00Z")
        );
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(existing));
        when(clockPort.now()).thenReturn(NOW);
        when(clinicConfigurationRepository.save(any(ClinicConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = updateService.update(command("Ten moi", LocalTime.of(9, 0), LocalTime.of(18, 0)));

        assertEquals("Ten moi", result.clinicName());
        assertEquals(LocalTime.of(9, 0), result.openingTime());
        assertEquals(NOW, existing.getUpdatedAt());
    }

    @Test
    void updateRejectsMissingNameAndInvalidWorkingHours() {
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(ValidationException.class,
                () -> updateService.update(command(" ", LocalTime.of(8, 0), LocalTime.of(17, 0))));
        assertThrows(ValidationException.class,
                () -> updateService.update(command("Phong kham", LocalTime.of(17, 0), LocalTime.of(8, 0))));
    }

    private static UpdateClinicConfigurationCommand command(
            String clinicName,
            LocalTime openingTime,
            LocalTime closingTime
    ) {
        return new UpdateClinicConfigurationCommand(
                clinicName,
                "Thanh pho Ho Chi Minh",
                "0900000000",
                openingTime,
                closingTime
        );
    }
}

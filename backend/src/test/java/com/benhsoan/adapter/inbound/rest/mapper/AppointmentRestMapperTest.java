package com.benhsoan.adapter.inbound.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.AppointmentSummaryResponse;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.AppointmentSummaryResult;

class AppointmentRestMapperTest {

    private AnonymizationModeState state;
    private AppointmentRestMapper mapper;

    @BeforeEach
    void setUp() {
        state = new AnonymizationModeState();
        mapper = new AppointmentRestMapper(state);
    }

    private AppointmentSummaryResult sampleResult(String patientCode, String patientName, String patientPhone) {
        return AppointmentSummaryResult.builder()
                .id(UUID.randomUUID())
                .appointmentCode("APT000001")
                .patientId(UUID.randomUUID())
                .patientCode(patientCode)
                .patientName(patientName)
                .patientPhone(patientPhone)
                .status(AppointmentStatus.CONFIRMED)
                .reason("Tai kham")
                .build();
    }

    @Test
    void returnsOriginalPatientNameAndPhoneWhenAnonymizationDisabled() {
        // Given
        state.setEnabled(false);
        AppointmentSummaryResult result = sampleResult("BN000001", "Nguyen Thi Banh", "0912345678");

        // When
        AppointmentSummaryResponse response = mapper.toResponse(result);

        // Then
        assertNotNull(response);
        assertEquals("BN000001", response.patientCode());
        assertEquals("Nguyen Thi Banh", response.patientName());
        assertEquals("0912345678", response.patientPhone());
    }

    @Test
    void masksPatientNameAndPhoneWhenAnonymizationEnabled() {
        // Given
        state.setEnabled(true);
        AppointmentSummaryResult result = sampleResult("BN000001", "Nguyen Thi Banh", "0912345678");

        // When
        AppointmentSummaryResponse response = mapper.toResponse(result);

        // Then
        assertNotNull(response);
        assertEquals("BN000001", response.patientCode());
        assertEquals("BỆNH NHÂN #BN000001", response.patientName());
        assertEquals("09******78", response.patientPhone());
    }

    @Test
    void masksPatientNameWithGenericFallbackWhenPatientCodeIsNull() {
        // Given
        state.setEnabled(true);
        AppointmentSummaryResult result = sampleResult(null, "Nguyen Thi Banh", "0912345678");

        // When
        AppointmentSummaryResponse response = mapper.toResponse(result);

        // Then
        assertNotNull(response);
        assertNull(response.patientCode());
        assertEquals("BỆNH NHÂN", response.patientName());
        assertEquals("09******78", response.patientPhone());
    }

    @Test
    void returnsNullWhenResultIsNull() {
        assertNull(mapper.toResponse((AppointmentSummaryResult) null));
    }
}

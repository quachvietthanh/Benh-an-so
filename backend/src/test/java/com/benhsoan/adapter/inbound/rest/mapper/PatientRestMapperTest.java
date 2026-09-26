package com.benhsoan.adapter.inbound.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.adapter.inbound.rest.response.patient.PatientResponse;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.port.dto.result.PatientResult;

class PatientRestMapperTest {

    private static final UUID ID = UUID.randomUUID();

    private AnonymizationModeState state;
    private PatientRestMapper mapper;

    @BeforeEach
    void setUp() {
        state = new AnonymizationModeState();
        mapper = new PatientRestMapper(state);
    }

    @Test
    void returnsOriginalPiiWhenDisabled() {
        PatientResponse response = mapper.toResponse(result());

        assertEquals("Nguyễn Văn A", response.fullName());
        assertEquals("0912345678", response.phone());
        assertEquals("123 Nguyễn Trãi, Hà Nội", response.address());
    }

    @Test
    void masksPiiWhenEnabled() {
        state.setEnabled(true);

        PatientResponse response = mapper.toResponse(result());

        assertEquals("BỆNH NHÂN #BN001", response.fullName());
        assertEquals("09******78", response.phone());
        assertEquals("[ĐỊA CHỈ ĐÃ ẨN DANH]", response.address());
    }

    @Test
    void masksEmergencyContactWhenEnabled() {
        PatientResult withEmergency = new PatientResult(
                ID, "BN001", "Nguyễn Văn A", null, null,
                "0912345678", null, "123 Nguyễn Trãi, Hà Nội",
                null, null, null, "Lê Thị B", "Vợ", "0987654321",
                true, null, null, false, null, null,
                false, null, null, false
        );

        PatientResponse normal = mapper.toResponse(withEmergency);
        assertEquals("Lê Thị B", normal.emergencyContact());
        assertEquals("Vợ", normal.emergencyRelationship());
        assertEquals("0987654321", normal.emergencyPhone());

        state.setEnabled(true);
        PatientResponse masked = mapper.toResponse(withEmergency);
        assertEquals("BỆNH NHÂN", masked.emergencyContact());
        assertEquals("Vợ", masked.emergencyRelationship());
        assertEquals("09******21", masked.emergencyPhone());
    }

    @Test
    void toggleIsReflectedAtRuntime() {
        PatientResponse original = mapper.toResponse(result());
        assertEquals("Nguyễn Văn A", original.fullName());

        state.setEnabled(true);
        PatientResponse masked = mapper.toResponse(result());
        assertEquals("BỆNH NHÂN #BN001", masked.fullName());

        state.setEnabled(false);
        PatientResponse restored = mapper.toResponse(result());
        assertEquals("Nguyễn Văn A", restored.fullName());
    }

    @Test
    void masksGuardianAndMinorConsentSignerWithDistinctGuardianLabelWhenEnabled() {
        PatientResult withGuardian = new PatientResult(
                ID, "BN002", "Bé Nguyễn Văn Con", java.time.LocalDate.of(2020, 1, 1), null,
                null, null, "123 Nguyễn Trãi, Hà Nội",
                null, null, null, null, null, null,
                "Nguyễn Văn Bố", "Bố", "0912345678", null, null,
                "Nguyễn Văn Bố", true, false,
                true, null, null, true, null, "v1.0",
                false, null, null, false
        );

        PatientResponse normal = mapper.toResponse(withGuardian);
        assertEquals("Nguyễn Văn Bố", normal.guardianName());
        assertEquals("0912345678", normal.guardianPhone());
        assertEquals("Nguyễn Văn Bố", normal.consentSignerName());

        state.setEnabled(true);
        PatientResponse masked = mapper.toResponse(withGuardian);
        assertEquals("BỆNH NHÂN #BN002", masked.fullName());
        assertEquals("GIÁM HỘ #BN002", masked.guardianName());
        assertEquals("09******78", masked.guardianPhone());
        assertEquals("GIÁM HỘ #BN002", masked.consentSignerName());
    }

    private static PatientResult result() {
        return new PatientResult(
                ID, "BN001", "Nguyễn Văn A", null, null,
                "0912345678", null, "123 Nguyễn Trãi, Hà Nội",
                null, null, null, null, null,
                true, null, null, false, null, null,
                false, null, null, false
        );
    }
}

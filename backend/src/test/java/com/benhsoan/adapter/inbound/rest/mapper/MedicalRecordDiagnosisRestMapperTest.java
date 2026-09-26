package com.benhsoan.adapter.inbound.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.adapter.inbound.rest.request.medicalrecord.ReplaceMedicalRecordDiagnosesRequest;

class MedicalRecordDiagnosisRestMapperTest {

    private final MedicalRecordDiagnosisRestMapper mapper = new MedicalRecordDiagnosisRestMapper();

    @Test
    void mapsPrimaryAndSecondaryDiagnosesToTheirDedicatedCommandTypes() {
        UUID primaryCatalogId = UUID.randomUUID();
        UUID secondaryCatalogId = UUID.randomUUID();

        var command = mapper.toCommand(new ReplaceMedicalRecordDiagnosesRequest(
                new ReplaceMedicalRecordDiagnosesRequest.PrimaryDiagnosisRequest(primaryCatalogId, "Primary note"),
                List.of(
                        new ReplaceMedicalRecordDiagnosesRequest.SecondaryDiagnosisRequest(secondaryCatalogId, null, "Catalog note"),
                        new ReplaceMedicalRecordDiagnosesRequest.SecondaryDiagnosisRequest(null, "Clinical observation", "Free-text note")
                )
        ));

        assertEquals(primaryCatalogId, command.primaryDiagnosis().diagnosisCatalogId());
        assertEquals("Primary note", command.primaryDiagnosis().note());
        assertEquals(secondaryCatalogId, command.secondaryDiagnoses().getFirst().diagnosisCatalogId());
        assertNull(command.secondaryDiagnoses().getFirst().name());
        assertNull(command.secondaryDiagnoses().get(1).diagnosisCatalogId());
        assertEquals("Clinical observation", command.secondaryDiagnoses().get(1).name());
    }
}

package com.benhsoan.persistence.adapterRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientMedicalHistoryRepository;
import com.benhsoan.persistence.jpaRepository.patient.PatientMedicalHistoryProjection;
import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;
import com.benhsoan.port.dto.result.MedicalHistoryItemResult;

@ExtendWith(MockitoExtension.class)
class PatientMedicalHistoryQueryAdapterTest {

    @Mock
    private JpaPatientMedicalHistoryRepository jpaRepository;

    @InjectMocks
    private PatientMedicalHistoryQueryAdapter adapter;

    @Test
    void mapsProjectionAndUsesRequestedPage() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant visitAt = Instant.parse("2026-08-20T02:00:00Z");
        GetPatientMedicalHistoryQuery query = new GetPatientMedicalHistoryQuery(
                patientId, null, null, 1, 10
        );
        PatientMedicalHistoryProjection projection = new PatientMedicalHistoryProjection(
                visitId, "VIS-001", VisitType.WALK_IN, VisitStatus.COMPLETED, visitAt,
                visitAt, visitAt, "Consultation", null, UUID.randomUUID(), "Dr. An",
                UUID.randomUUID(), MedicalRecordStatus.LOCKED, "Headache", "Recovered"
        );
        when(jpaRepository.findMedicalHistory(eq(patientId), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        Page<MedicalHistoryItemResult> result = adapter.findMedicalHistory(query);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findMedicalHistory(eq(patientId), eq(null), eq(null), pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
        assertEquals(visitId, result.getContent().getFirst().visitId());
        assertEquals("Dr. An", result.getContent().getFirst().doctorName());
        assertEquals(MedicalRecordStatus.LOCKED, result.getContent().getFirst().medicalRecordStatus());
    }
}

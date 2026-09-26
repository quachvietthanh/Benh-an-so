package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionsQuery;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class SearchPrescriptionsServiceTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PrescriptionWarningLogRepository warningLogRepository;
    @Mock private PrescriptionResultMapper resultMapper;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private VisitRepository visitRepository;

    private SearchPrescriptionsService service;

    @BeforeEach
    void setUp() {
        var accessValidator = new PrescriptionReadAccessValidator(
                currentUserPort,
                medicalRecordRepository,
                visitRepository
        );
        service = new SearchPrescriptionsService(
                prescriptionRepository,
                warningLogRepository,
                accessValidator,
                resultMapper
        );
    }

    @Test
    void returnsRequestedStatusPageOldestFirstForPharmacist() {
        Prescription prescription = mock(Prescription.class);
        PrescriptionResult mapped = mock(PrescriptionResult.class);
        var pageable = PageRequest.of(
                1,
                10,
                Sort.by(Sort.Direction.ASC, "prescribedAt")
        );
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(prescriptionRepository.findByStatus(PrescriptionStatus.PENDING_DISPENSE, pageable))
                .thenReturn(new PageImpl<>(List.of(prescription), pageable, 11));
        when(warningLogRepository.findByPrescriptionId(prescription.getId()))
                .thenReturn(List.of());
        when(resultMapper.toResult(prescription, List.of())).thenReturn(mapped);

        var result = service.search(new SearchPrescriptionsQuery(
                PrescriptionStatus.PENDING_DISPENSE,
                1,
                10
        ));

        assertSame(mapped, result.getContent().getFirst());
        verify(prescriptionRepository).findByStatus(PrescriptionStatus.PENDING_DISPENSE, pageable);
    }

    @Test
    void rejectsDoctorBeforeReadingRepository() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.search(
                new SearchPrescriptionsQuery(PrescriptionStatus.PENDING_DISPENSE, 0, 20)
        ));

        verify(prescriptionRepository, never()).findByStatus(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}

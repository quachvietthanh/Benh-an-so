package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.CheckMaxDailyDoseCommand;
import com.benhsoan.port.dto.command.prescription.CheckMaxDailyDoseItemCommand;
import com.benhsoan.port.dto.result.MaxDailyDoseCheckResult;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class CheckMaxDailyDoseServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T02:00:00Z");

    @Mock
    private MedicineRepository medicineRepository;
    @Mock
    private PrescriptionClinicalContextValidator clinicalContextValidator;
    @Mock
    private CurrentUserPort currentUserPort;

    private CheckMaxDailyDoseService service;
    private UUID medicalRecordId;
    private UUID medicineId;

    @BeforeEach
    void setUp() {
        service = new CheckMaxDailyDoseService(
                medicineRepository,
                clinicalContextValidator,
                currentUserPort
        );
        medicalRecordId = UUID.randomUUID();
        medicineId = UUID.randomUUID();
        lenient().when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void exceedance_returnsWarning() {
        when(medicineRepository.findAllById(anyList()))
                .thenReturn(List.of(medicine(medicineId, "500", "2000")));

        MaxDailyDoseCheckResult result = service.check(command(
                List.of(item(medicineId, new BigDecimal("2"), 3))));

        assertTrue(result.missingData().isEmpty());
        assertEquals(1, result.warnings().size());
        assertEquals("Paracetamol", result.warnings().getFirst().activeIngredient());
        assertEquals(0, new BigDecimal("3000").compareTo(result.warnings().getFirst().totalDailyDoseMg()));
        assertEquals(0, new BigDecimal("2000").compareTo(result.warnings().getFirst().maxDailyDoseMg()));
    }

    @Test
    void withinMax_returnsNoWarning() {
        when(medicineRepository.findAllById(anyList()))
                .thenReturn(List.of(medicine(medicineId, "500", "2000")));

        MaxDailyDoseCheckResult result = service.check(command(
                List.of(item(medicineId, new BigDecimal("1"), 3))));

        assertTrue(result.warnings().isEmpty());
        assertTrue(result.missingData().isEmpty());
    }

    @Test
    void missingMaxDose_returnsMissingData() {
        when(medicineRepository.findAllById(anyList()))
                .thenReturn(List.of(medicine(medicineId, "500", null)));

        MaxDailyDoseCheckResult result = service.check(command(
                List.of(item(medicineId, new BigDecimal("2"), 3))));

        assertTrue(result.warnings().isEmpty());
        assertEquals(1, result.missingData().size());
    }

    @Test
    void missingStrength_returnsMissingData() {
        when(medicineRepository.findAllById(anyList()))
                .thenReturn(List.of(medicine(medicineId, null, "2000")));

        MaxDailyDoseCheckResult result = service.check(command(
                List.of(item(medicineId, new BigDecimal("2"), 3))));

        assertTrue(result.warnings().isEmpty());
        assertEquals(1, result.missingData().size());
    }

    @Test
    void unauthorizedDoctor_rejected() {
        when(clinicalContextValidator.requireEditableRecordForDoctor(any(), any()))
                .thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> service.check(command(
                List.of(item(medicineId, new BigDecimal("2"), 3)))));
    }

    @Test
    void nullCommand_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.check(null));
    }

    @Test
    void medicineNotFound_throwsValidationException() {
        when(medicineRepository.findAllById(anyList())).thenReturn(List.of());

        assertThrows(ValidationException.class, () -> service.check(command(
                List.of(item(medicineId, new BigDecimal("2"), 3)))));
    }

    private CheckMaxDailyDoseCommand command(List<CheckMaxDailyDoseItemCommand> items) {
        return new CheckMaxDailyDoseCommand(medicalRecordId, items);
    }

    private CheckMaxDailyDoseItemCommand item(UUID medicineId, BigDecimal singleDoseQuantity, int frequency) {
        return new CheckMaxDailyDoseItemCommand(medicineId, singleDoseQuantity, frequency);
    }

    private Medicine medicine(UUID id, String strengthValueMg, String maxDailyDoseMg) {
        return Medicine.restore(
                id,
                "MED-001",
                "Paracetamol 500",
                "Paracetamol",
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                true,
                NOW,
                null,
                0,
                20,
                false,
                strengthValueMg == null ? null : new BigDecimal(strengthValueMg),
                maxDailyDoseMg == null ? null : new BigDecimal(maxDailyDoseMg)
        );
    }
}

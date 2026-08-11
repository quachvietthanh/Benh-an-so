package com.benhsoan.application.ucservice.medicine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicine.CreateMedicineCommand;
import com.benhsoan.port.dto.command.medicine.SearchMedicinesQuery;
import com.benhsoan.port.dto.command.medicine.UpdateMedicineCommand;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class MedicineApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-06T02:00:00Z");

    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final MedicineManagementAuthorizer authorizer =
            new MedicineManagementAuthorizer(currentUserPort);
    private final MedicineResultMapper resultMapper = new MedicineResultMapper();

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
        when(medicineRepository.save(any(Medicine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsMedicineForPharmacist() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        CreateMedicineService service = new CreateMedicineService(
                medicineRepository,
                authorizer,
                resultMapper,
                clockPort
        );

        var result = service.create(new CreateMedicineCommand(
                " med-001 ",
                " Paracetamol ",
                " Acetaminophen ",
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                20
        ));

        assertEquals("med-001", result.medicineCode());
        assertEquals("Paracetamol", result.medicineName());
        assertTrue(result.active());
        verify(medicineRepository).existsByMedicineCode("med-001");
        verify(medicineRepository).existsByMedicineNameAndActiveIngredient(
                "paracetamol",
                "acetaminophen",
                null
        );
    }

    @Test
    void rejectsDuplicateMedicineCodeBeforeSave() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(medicineRepository.existsByMedicineCode("med-001")).thenReturn(true);
        CreateMedicineService service = new CreateMedicineService(
                medicineRepository,
                authorizer,
                resultMapper,
                clockPort
        );

        assertThrows(ValidationException.class, () -> service.create(
                new CreateMedicineCommand(
                        "med-001",
                        "Paracetamol",
                        "Acetaminophen",
                        "500 mg",
                        DosageForm.TABLET,
                        "vien",
                        AdministrationRoute.ORAL,
                        20
                )
        ));
    }

    @Test
    void rejectsDuplicateMedicineCodeIgnoringCaseAndOuterWhitespace() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(medicineRepository.existsByMedicineCode("med-001")).thenReturn(true);
        CreateMedicineService service = new CreateMedicineService(
                medicineRepository,
                authorizer,
                resultMapper,
                clockPort
        );

        assertThrows(ValidationException.class, () -> service.create(
                new CreateMedicineCommand(
                        " MED-001 ",
                        "Paracetamol",
                        "Acetaminophen",
                        "500 mg",
                        DosageForm.TABLET,
                        "vien",
                        AdministrationRoute.ORAL,
                        20
                )
        ));
    }

    @Test
    void searchesMedicinesWithPageableForPharmacist() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        Medicine medicine = activeMedicine(UUID.randomUUID(), "MED-001", "Paracetamol");
        var pageable = PageRequest.of(0, 20, Sort.by("medicineName", "medicineCode"));
        when(medicineRepository.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(medicine), pageable, 1));
        SearchMedicinesService service = new SearchMedicinesService(
                medicineRepository,
                authorizer,
                resultMapper
        );

        var result = service.search(new SearchMedicinesQuery(
                "  para  ",
                true,
                pageable
        ));

        assertEquals(1, result.getTotalElements());
        assertEquals("MED-001", result.getContent().getFirst().medicineCode());
        assertEquals("Paracetamol", result.getContent().getFirst().medicineName());
        verify(medicineRepository).search(
                new com.benhsoan.port.outbound.repository.medicine.MedicineSearchCriteria(
                        "para",
                        null,
                        null,
                        true
                ),
                pageable
        );
    }

    @Test
    void updatesMedicineAndRejectsDuplicateNameIngredient() {
        UUID medicineId = UUID.randomUUID();
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(medicineRepository.findById(medicineId))
                .thenReturn(Optional.of(activeMedicine(medicineId, "MED-001", "Paracetamol")));
        when(medicineRepository.existsByMedicineNameAndActiveIngredient(
                "ibuprofen",
                "ibuprofen",
                medicineId
        )).thenReturn(true);
        UpdateMedicineService service = new UpdateMedicineService(
                medicineRepository,
                authorizer,
                resultMapper,
                clockPort
        );

        assertThrows(ValidationException.class, () -> service.update(
                new UpdateMedicineCommand(
                        medicineId,
                        "Ibuprofen",
                        "Ibuprofen",
                        "400 mg",
                        DosageForm.TABLET,
                        "vien",
                        AdministrationRoute.ORAL,
                        20
                )
        ));
    }

    @Test
    void activatesAndDeactivatesMedicine() {
        UUID medicineId = UUID.randomUUID();
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(medicineRepository.findById(medicineId))
                .thenReturn(Optional.of(inactiveMedicine(medicineId, "MED-001", "Paracetamol")))
                .thenReturn(Optional.of(activeMedicine(medicineId, "MED-001", "Paracetamol")));

        var activateService = new ActivateMedicineService(
                medicineRepository,
                authorizer,
                resultMapper,
                clockPort
        );
        var deactivateService = new DeactivateMedicineService(
                medicineRepository,
                authorizer,
                resultMapper,
                clockPort
        );

        var activated = activateService.activate(medicineId);
        var deactivated = deactivateService.deactivate(medicineId);

        assertTrue(activated.active());
        assertFalse(deactivated.active());
    }

    @Test
    void rejectsMedicineManagementForNonPharmacist() {
        CreateMedicineService service = new CreateMedicineService(
                medicineRepository,
                authorizer,
                resultMapper,
                clockPort
        );

        assertThrows(AccessDeniedException.class, () -> service.create(
                new CreateMedicineCommand(
                        "MED-001",
                        "Paracetamol",
                        "Acetaminophen",
                        "500 mg",
                        DosageForm.TABLET,
                        "vien",
                        AdministrationRoute.ORAL,
                        20
                )
        ));
    }

    @Test
    void rejectsMedicineManagementForAdminWithoutPharmacistRole() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);

        CreateMedicineService service = new CreateMedicineService(
                medicineRepository,
                authorizer,
                resultMapper,
                clockPort
        );

        assertThrows(AccessDeniedException.class, () -> service.create(
                new CreateMedicineCommand(
                        "MED-ADMIN-001",
                        "Paracetamol",
                        "Acetaminophen",
                        "500 mg",
                        DosageForm.TABLET,
                        "vien",
                        AdministrationRoute.ORAL,
                        20
                )
        ));
    }

    private Medicine activeMedicine(UUID id, String code, String name) {
        return Medicine.restore(
                id,
                code,
                name,
                name,
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                true,
                NOW.minusSeconds(60),
                null,
                0,
                20
        );
    }

    private Medicine inactiveMedicine(UUID id, String code, String name) {
        return Medicine.restore(
                id,
                code,
                name,
                name,
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                false,
                NOW.minusSeconds(60),
                null,
                0,
                20
        );
    }
}

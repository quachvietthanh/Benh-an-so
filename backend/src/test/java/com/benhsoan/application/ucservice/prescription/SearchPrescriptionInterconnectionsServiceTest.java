package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionInterconnectionsQuery;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class SearchPrescriptionInterconnectionsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T03:00:00Z");

    @Test
    void adminCanFilterAndGetsNewestInterconnectionsFirst() {
        PrescriptionRepository repository = mock(PrescriptionRepository.class);
        PrescriptionDisplayContextResolver resolver = mock(PrescriptionDisplayContextResolver.class);
        CurrentUserPort currentUser = mock(CurrentUserPort.class);
        when(currentUser.hasRole("ADMIN")).thenReturn(true);
        Prescription prescription = failedPrescription();
        when(repository.findByInterconnectionStatus(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(prescription)));
        when(resolver.resolve(any(), any())).thenReturn(
                new PrescriptionDisplayContextResolver.PrescriptionDisplayContext(
                        UUID.randomUUID(), "VISIT-001", UUID.randomUUID(), "PAT-001", "Nguyen Van A", "Dr. B"));
        var service = new SearchPrescriptionInterconnectionsService(repository, resolver, currentUser);

        var page = service.search(new SearchPrescriptionInterconnectionsQuery(
                InterconnectionStatus.FAILED, NOW.minusSeconds(3600), NOW, 0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals("RX000001", page.getContent().getFirst().prescriptionCode());
        assertEquals(InterconnectionStatus.FAILED, page.getContent().getFirst().interconnectionStatus());
        org.mockito.ArgumentCaptor<Pageable> pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByInterconnectionStatus(
                org.mockito.ArgumentMatchers.eq(InterconnectionStatus.FAILED),
                org.mockito.ArgumentMatchers.eq(NOW.minusSeconds(3600)), org.mockito.ArgumentMatchers.eq(NOW),
                pageable.capture());
        assertEquals(org.springframework.data.domain.Sort.Direction.DESC,
                pageable.getValue().getSort().getOrderFor("lastInterconnectionAt").getDirection());
    }

    @Test
    void nonAdminCannotSearch() {
        CurrentUserPort currentUser = mock(CurrentUserPort.class);
        var service = new SearchPrescriptionInterconnectionsService(
                mock(PrescriptionRepository.class), mock(PrescriptionDisplayContextResolver.class), currentUser);

        assertThrows(AccessDeniedException.class, () -> service.search(
                new SearchPrescriptionInterconnectionsQuery(InterconnectionStatus.NOT_SENT, null, null, 0, 20)));
    }

    private Prescription failedPrescription() {
        UUID id = UUID.randomUUID();
        PrescriptionItem item = PrescriptionItem.restore(UUID.randomUUID(), id, UUID.randomUUID(), "Paracetamol",
                "Paracetamol", "500 mg", "vien", "1 vien", 2, AdministrationRoute.ORAL, 5, 10,
                null, NOW.minusSeconds(600), null);
        return Prescription.restore(id, "RX000001", UUID.randomUUID(), PrescriptionStatus.PENDING_DISPENSE, null,
                UUID.randomUUID(), NOW.minusSeconds(600), null, null, InterconnectionStatus.FAILED,
                NOW.minusSeconds(60), "Gateway unavailable", null, List.of(item));
    }
}

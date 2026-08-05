package com.benhsoan.application.ucservice.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.clinical.exception.ClinicalOrderLockedMedicalRecordException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidVisitException;
import com.benhsoan.domain.clinical.exception.ClinicalServiceUnavailableException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.clinical.CreateClinicalOrderCommand;
import com.benhsoan.port.dto.command.clinical.GetClinicalOrdersByVisitQuery;
import com.benhsoan.port.dto.command.clinical.SearchClinicalServiceCatalogQuery;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ClinicalOrderApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");

    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private ClinicalServiceCatalogRepository clinicalServiceCatalogRepository;
    @Mock private ClinicalOrderRepository clinicalOrderRepository;
    @Mock private ClinicalOrderItemRepository clinicalOrderItemRepository;
    @Mock private ClinicalOrderAuthorizationService authorizationService;
    @Mock private ClinicalOrderAuditService auditService;
    @Mock private ClockPort clockPort;
    @Spy private ClinicalOrderResultMapper resultMapper = new ClinicalOrderResultMapper();

    @InjectMocks private CreateClinicalOrderService createClinicalOrderService;
    @InjectMocks private SearchClinicalServiceCatalogService searchClinicalServiceCatalogService;
    @InjectMocks private GetClinicalOrdersByVisitService getClinicalOrdersByVisitService;

    @Test
    void createsOrderAndItemsInOneTransaction() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Visit visit = activeVisit(visitId, patientId, actorId);
        MedicalRecord record = editableMedicalRecord(visitId, actorId);
        ClinicalServiceCatalog catalog = ClinicalServiceCatalog.restore(serviceId, "LAB-GLU", "Blood glucose",
                ClinicalServiceType.LAB_TEST, ClinicalResultDataType.NUMBER, "mmol/L", null, null, true, NOW, null);
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record));
        when(clinicalServiceCatalogRepository.findActiveByIdIn(List.of(serviceId))).thenReturn(List.of(catalog));
        when(clockPort.now()).thenReturn(NOW);
        when(clinicalOrderRepository.existsByOrderCode(any())).thenReturn(false);
        when(clinicalOrderRepository.save(any(ClinicalOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clinicalOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = createClinicalOrderService.createOrder(visitId,
                new CreateClinicalOrderCommand("Check glucose", List.of(
                        new CreateClinicalOrderCommand.OrderItemCommand(serviceId, "Fasting")
                )));

        assertEquals(visitId, result.visitId());
        assertEquals("LAB-GLU", result.items().getFirst().serviceCode());
        verify(clinicalOrderItemRepository).saveAll(any());
        verify(auditService).recordCreated(patientId, visitId, record.getId(), actorId, NOW);
    }

    @Test
    void rejectsOrderForLockedMedicalRecord() {
        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        MedicalRecord record = editableMedicalRecord(visitId, actorId);
        record.lock(actorId, NOW);
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit(visitId, UUID.randomUUID(), actorId)));
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record));

        assertThrows(ClinicalOrderLockedMedicalRecordException.class, () -> createClinicalOrderService.createOrder(visitId,
                new CreateClinicalOrderCommand("Reason", List.of(
                        new CreateClinicalOrderCommand.OrderItemCommand(UUID.randomUUID(), null)
                ))));
    }

    @Test
    void rejectsOrderWhenVisitDoesNotExist() {
        UUID visitId = UUID.randomUUID();
        when(authorizationService.requireWriteAccess()).thenReturn(UUID.randomUUID());
        when(visitRepository.findById(visitId)).thenReturn(Optional.empty());

        assertThrows(VisitNotFoundException.class, () -> createClinicalOrderService.createOrder(visitId, command()));
    }

    @Test
    void rejectsOrderForInactiveVisit() {
        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Visit cancelledVisit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.CANCELLED, NOW, null, NOW, "Consultation", null, actorId, NOW, null);
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(cancelledVisit));

        assertThrows(ClinicalOrderInvalidVisitException.class,
                () -> createClinicalOrderService.createOrder(visitId, command()));
    }

    @Test
    void rejectsOrderWhenVisitHasNoMedicalRecord() {
        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit(visitId, UUID.randomUUID(), actorId)));
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.empty());

        assertThrows(MedicalRecordNotFoundException.class,
                () -> createClinicalOrderService.createOrder(visitId, command()));
    }

    @Test
    void rejectsInactiveOrUnknownClinicalService() {
        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit(visitId, UUID.randomUUID(), actorId)));
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(editableMedicalRecord(visitId, actorId)));
        when(clinicalServiceCatalogRepository.findActiveByIdIn(List.of(serviceId))).thenReturn(List.of());

        assertThrows(ClinicalServiceUnavailableException.class, () -> createClinicalOrderService.createOrder(visitId,
                new CreateClinicalOrderCommand("Reason", List.of(
                        new CreateClinicalOrderCommand.OrderItemCommand(serviceId, null)
                ))));
    }

    @Test
    void rejectsDuplicateClinicalServiceInOneOrder() {
        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit(visitId, UUID.randomUUID(), actorId)));
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(editableMedicalRecord(visitId, actorId)));

        assertThrows(ClinicalServiceUnavailableException.class, () -> createClinicalOrderService.createOrder(visitId,
                new CreateClinicalOrderCommand("Reason", List.of(
                        new CreateClinicalOrderCommand.OrderItemCommand(serviceId, null),
                        new CreateClinicalOrderCommand.OrderItemCommand(serviceId, "Duplicate")
                ))));
    }

    @Test
    void searchesOnlyActiveCatalogServices() {
        UUID actorId = UUID.randomUUID();
        ClinicalServiceCatalog catalog = ClinicalServiceCatalog.restore(UUID.randomUUID(), "LAB-GLU", "Blood glucose",
                ClinicalServiceType.LAB_TEST, ClinicalResultDataType.NUMBER, "mmol/L", null, null, true, NOW, null);
        when(authorizationService.requireReadAccess()).thenReturn(actorId);
        when(clinicalServiceCatalogRepository.findActiveByKeyword("glucose", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(catalog)));

        var result = searchClinicalServiceCatalogService.search(new SearchClinicalServiceCatalogQuery("glucose", 0, 20));

        assertEquals("LAB-GLU", result.getContent().getFirst().serviceCode());
    }

    @Test
    void readsOrderItemsWithOneBulkLookup() {
        UUID visitId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ClinicalOrder order = ClinicalOrder.restore(orderId, "ORD-123", visitId, UUID.randomUUID(), UUID.randomUUID(),
                actorId, null, com.benhsoan.domain.clinical.enums.ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
        ClinicalOrderItem item = ClinicalOrderItem.restore(UUID.randomUUID(), orderId, UUID.randomUUID(), "LAB-GLU",
                "Blood glucose", null, com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus.PENDING, NOW, null);
        when(authorizationService.requireReadAccess()).thenReturn(actorId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit(visitId, UUID.randomUUID(), actorId)));
        when(clinicalOrderRepository.findByVisitId(visitId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(orderId))).thenReturn(List.of(item));

        var result = getClinicalOrdersByVisitService.getOrdersByVisit(new GetClinicalOrdersByVisitQuery(visitId, 0, 20));

        assertEquals("LAB-GLU", result.getContent().getFirst().items().getFirst().serviceCode());
        verify(clinicalOrderItemRepository).findByClinicalOrderIdIn(List.of(orderId));
    }

    private Visit activeVisit(UUID visitId, UUID patientId, UUID actorId) {
        return Visit.restore(visitId, "VIS-001", patientId, UUID.randomUUID(), null, null, VisitType.WALK_IN,
                VisitStatus.IN_PROGRESS, NOW, NOW, null, "Consultation", null, actorId, NOW, null);
    }

    private MedicalRecord editableMedicalRecord(UUID visitId, UUID actorId) {
        return MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null, "Stable", actorId, NOW);
    }

    private CreateClinicalOrderCommand command() {
        return new CreateClinicalOrderCommand("Reason", List.of(
                new CreateClinicalOrderCommand.OrderItemCommand(UUID.randomUUID(), null)
        ));
    }
}

package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.DiscountRequest;
import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.exception.PendingDiscountApprovalException;
import com.benhsoan.domain.billing.exception.SelfApprovalNotAllowedException;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.adapterRepository.auditlog.AuditLogRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.billing.DiscountRequestRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.billing.InvoiceRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.billing.PaymentRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.billing.PaymentServiceFeeRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.visit.VisitRepositoryAdapter;
import com.benhsoan.persistence.entity.patient.PatientEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;
import com.benhsoan.persistence.mapper.auditlog.AuditLogPersistenceMapper;
import com.benhsoan.persistence.mapper.billing.DiscountRequestPersistenceMapper;
import com.benhsoan.persistence.mapper.billing.InvoiceLinePersistenceMapper;
import com.benhsoan.persistence.mapper.billing.InvoicePersistenceMapper;
import com.benhsoan.persistence.mapper.billing.PaymentMethodItemPersistenceMapper;
import com.benhsoan.persistence.mapper.billing.PaymentPersistenceMapper;
import com.benhsoan.persistence.mapper.billing.PaymentServiceFeePersistenceMapper;
import com.benhsoan.persistence.mapper.visit.VisitPersistenceMapper;
import com.benhsoan.port.dto.command.billing.CreateDiscountRequestCommand;
import com.benhsoan.port.dto.command.billing.CreateInvoiceCommand;
import com.benhsoan.port.dto.command.billing.RecordPaymentCommand;
import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.dto.result.PaymentResult;
import com.benhsoan.port.outbound.generator.InvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@DataJpaTest(properties = {
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
                DiscountRequestRepositoryAdapter.class,
                DiscountRequestPersistenceMapper.class,
                PaymentRepositoryAdapter.class,
                PaymentPersistenceMapper.class,
                PaymentMethodItemPersistenceMapper.class,
                InvoiceRepositoryAdapter.class,
                InvoicePersistenceMapper.class,
                InvoiceLinePersistenceMapper.class,
                PaymentServiceFeeRepositoryAdapter.class,
                PaymentServiceFeePersistenceMapper.class,
                AuditLogRepositoryAdapter.class,
                AuditLogPersistenceMapper.class,
                VisitRepositoryAdapter.class,
                VisitPersistenceMapper.class,
                BillingAccessDeniedAuditWriter.class,
                PaymentResultMapper.class,
                InvoiceResultMapper.class,
                DiscountRequestResultMapper.class
})
class InvoiceDiscountWorkflowIntegrationTest {

        @Autowired
        private DiscountRequestRepositoryAdapter discountRequestRepository;
        @Autowired
        private PaymentRepositoryAdapter paymentRepository;
        @Autowired
        private InvoiceRepositoryAdapter invoiceRepository;
        @Autowired
        private PaymentServiceFeeRepositoryAdapter paymentServiceFeeRepository;
        @Autowired
        private AuditLogRepositoryAdapter auditLogRepository;
        @Autowired
        private VisitRepositoryAdapter visitRepository;
        @Autowired
        private BillingAccessDeniedAuditWriter accessDeniedAuditWriter;

        @Autowired
        private JpaAuditLogRepository jpaAuditLogRepository;
        @Autowired
        private JpaVisitRepository jpaVisitRepository;
        @Autowired
        private JpaPatientRepository jpaPatientRepository;

        @MockitoBean
        private ClinicalServiceFeeCalculator clinicalServiceFeeCalculator;
        @MockitoBean
        private MedicalRecordRepository medicalRecordRepository;
        @MockitoBean
        private PrescriptionRepository prescriptionRepository;
        @MockitoBean
        private InvoiceCodeGenerator invoiceCodeGenerator;
        @MockitoBean
        private CurrentUserPort currentUserPort;
        @MockitoBean
        private ClockPort clockPort;

        private CreateDiscountRequestService createDiscountRequestService;
        private ApproveDiscountRequestService approveDiscountRequestService;
        private RecordPaymentService recordPaymentService;
        private CreateInvoiceService createInvoiceService;

        private final Instant fixedNow = Instant.parse("2026-09-21T10:00:00Z");

        @BeforeEach
        void setUp() {
                createDiscountRequestService = new CreateDiscountRequestService(
                                visitRepository,
                                paymentRepository,
                                discountRequestRepository,
                                clinicalServiceFeeCalculator,
                                currentUserPort,
                                clockPort,
                                auditLogRepository,
                                new DiscountRequestResultMapper());

                approveDiscountRequestService = new ApproveDiscountRequestService(
                                discountRequestRepository,
                                currentUserPort,
                                clockPort,
                                auditLogRepository,
                                accessDeniedAuditWriter,
                                new DiscountRequestResultMapper());

                recordPaymentService = new RecordPaymentService(
                                visitRepository,
                                medicalRecordRepository,
                                prescriptionRepository,
                                paymentRepository,
                                currentUserPort,
                                clockPort,
                                auditLogRepository,
                                new PaymentResultMapper(),
                                clinicalServiceFeeCalculator,
                                paymentServiceFeeRepository,
                                discountRequestRepository);

                createInvoiceService = new CreateInvoiceService(
                                paymentRepository,
                                invoiceRepository,
                                invoiceCodeGenerator,
                                currentUserPort,
                                clockPort,
                                auditLogRepository,
                                new InvoiceResultMapper(),
                                paymentServiceFeeRepository,
                                discountRequestRepository);

                when(clockPort.now()).thenReturn(fixedNow);
                when(clinicalServiceFeeCalculator.calculate(any(), any())).thenReturn(List.of());
                when(clinicalServiceFeeCalculator.total(any())).thenReturn(BigDecimal.ZERO);
                when(medicalRecordRepository.findByVisitId(any())).thenReturn(Optional.empty());
        }

        @Test
        void pendingDiscountBlocksPaymentAndInvoiceCreation() {
                UUID visitId = createVisitInDb();
                UUID receptionistId = UUID.randomUUID();

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);

                DiscountRequestResult requestResult = createDiscountRequestService
                                .create(new CreateDiscountRequestCommand(
                                                visitId,
                                                DiscountType.PERCENTAGE,
                                                new BigDecimal("20"),
                                                new BigDecimal("200000"),
                                                "Chính sách hỗ trợ"));
                assertEquals(DiscountRequestStatus.PENDING, requestResult.status());

                // AC TC-02 & QTN-37: Payment is blocked while discount is pending
                assertThrows(
                                PendingDiscountApprovalException.class,
                                () -> recordPaymentService.record(new RecordPaymentCommand(
                                                visitId,
                                                new BigDecimal("200000"),
                                                BigDecimal.ZERO,
                                                new BigDecimal("160000"),
                                                PaymentMethod.CASH)));

                // Invoice creation is also blocked
                assertThrows(
                                PendingDiscountApprovalException.class,
                                () -> createInvoiceService.create(new CreateInvoiceCommand(visitId, null)));
        }

        @Test
        void blocksSelfApprovalAndRecordsAccessDeniedAudit() {
                UUID visitId = createVisitInDb();
                UUID requesterId = UUID.randomUUID();

                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(requesterId);

                DiscountRequestResult requestResult = createDiscountRequestService
                                .create(new CreateDiscountRequestCommand(
                                                visitId,
                                                DiscountType.PERCENTAGE,
                                                new BigDecimal("10"),
                                                new BigDecimal("100000"),
                                                "Ưu đãi"));

                // Requester tries to approve their own request (QTN-37 / AC TC-03)
                assertThrows(
                                SelfApprovalNotAllowedException.class,
                                () -> approveDiscountRequestService.approve(requestResult.id()));

                // Verify audit log has ACCESS_DENIED recorded
                var auditLogs = jpaAuditLogRepository.findAll();
                boolean hasAccessDenied = auditLogs.stream()
                                .anyMatch(log -> log.getActionType() == ActionType.ACCESS_DENIED &&
                                                log.getResourceType() == ResourceType.DISCOUNT_REQUEST &&
                                                log.getResourceId().equals(requestResult.id()));
                assertTrue(hasAccessDenied, "Audit log must contain ACCESS_DENIED entry for self-approval attempt.");
        }

        @Test
        void completeDiscountWorkflowFromApprovalToInvoice() {
                UUID visitId = createVisitInDb();
                UUID requesterId = UUID.randomUUID();
                UUID managerId = UUID.randomUUID();

                // 1. Receptionist requests discount
                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(requesterId);

                DiscountRequestResult requestResult = createDiscountRequestService
                                .create(new CreateDiscountRequestCommand(
                                                visitId,
                                                DiscountType.PERCENTAGE,
                                                new BigDecimal("50"),
                                                new BigDecimal("200000"),
                                                "Giảm 50%"));
                assertEquals(new BigDecimal("100000.00"), requestResult.discountAmount());
                assertEquals(new BigDecimal("100000.00"), requestResult.finalAmount());

                // 2. Manager approves discount
                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
                when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(managerId);

                DiscountRequestResult approved = approveDiscountRequestService.approve(requestResult.id());
                assertEquals(DiscountRequestStatus.APPROVED, approved.status());
                assertEquals(managerId, approved.approvedBy());

                // 3. Receptionist records payment: 200,000 total - 100,000 discount = 100,000
                // paid
                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(requesterId);

                PaymentResult paymentResult = recordPaymentService.record(new RecordPaymentCommand(
                                visitId,
                                new BigDecimal("200000"),
                                BigDecimal.ZERO,
                                new BigDecimal("100000"),
                                PaymentMethod.CASH));

                assertEquals(new BigDecimal("200000"), paymentResult.totalAmount());
                assertEquals(new BigDecimal("100000.00"), paymentResult.discountAmount());
                assertEquals(new BigDecimal("100000"), paymentResult.amountPaid());
                assertEquals(requestResult.id(), paymentResult.discountRequestId());

                // 4. Receptionist creates invoice
                when(invoiceCodeGenerator.generate()).thenReturn("HD000088");

                InvoiceResult invoiceResult = createInvoiceService.create(new CreateInvoiceCommand(visitId, null));

                assertEquals("HD000088", invoiceResult.invoiceCode());
                assertEquals(new BigDecimal("100000.00"), invoiceResult.totalAmount());
                assertEquals(2, invoiceResult.lines().size());

                var discountLine = invoiceResult.lines().stream()
                                .filter(l -> l.lineType() == InvoiceLineType.DISCOUNT)
                                .findFirst()
                                .orElseThrow();
                assertEquals(new BigDecimal("-100000.00"), discountLine.amount());

                // Verify DiscountRequest now has invoiceId linked
                DiscountRequest updatedRequest = discountRequestRepository.findById(requestResult.id()).orElseThrow();
                assertEquals(invoiceResult.id(), updatedRequest.getInvoiceId());
        }

        @Test
        void fullFreeDiscountWorkflowWithZeroPaymentAndZeroInvoice() {
                UUID visitId = createVisitInDb();
                UUID requesterId = UUID.randomUUID();
                UUID managerId = UUID.randomUUID();

                // 1. Receptionist requests full free exemption (100%)
                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(requesterId);

                DiscountRequestResult requestResult = createDiscountRequestService
                                .create(new CreateDiscountRequestCommand(
                                                visitId,
                                                DiscountType.FULL_FREE,
                                                BigDecimal.ZERO,
                                                new BigDecimal("300000"),
                                                "Miễn phí hoàn toàn 100%"));
                assertEquals(new BigDecimal("300000"), requestResult.discountAmount());
                assertEquals(BigDecimal.ZERO, requestResult.finalAmount());

                // 2. Manager approves
                when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
                approveDiscountRequestService.approve(requestResult.id());

                // 3. Record payment: amountPaid = 0
                when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(requesterId);

                PaymentResult paymentResult = recordPaymentService.record(new RecordPaymentCommand(
                                visitId,
                                new BigDecimal("300000"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                PaymentMethod.CASH));

                assertEquals(new BigDecimal("300000"), paymentResult.totalAmount());
                assertEquals(new BigDecimal("300000"), paymentResult.discountAmount());
                assertEquals(BigDecimal.ZERO, paymentResult.amountPaid());

                // 4. Create original invoice: totalAmount = 0
                when(invoiceCodeGenerator.generate()).thenReturn("HD000089");
                InvoiceResult invoiceResult = createInvoiceService.create(new CreateInvoiceCommand(visitId, null));

                assertEquals(BigDecimal.ZERO, invoiceResult.totalAmount());
                assertEquals(2, invoiceResult.lines().size());

                var discountLine = invoiceResult.lines().stream()
                                .filter(l -> l.lineType() == InvoiceLineType.DISCOUNT)
                                .findFirst()
                                .orElseThrow();
                assertEquals(new BigDecimal("-300000"), discountLine.amount());
        }

        private UUID createVisitInDb() {
                UUID patientId = UUID.randomUUID();
                jpaPatientRepository.saveAndFlush(PatientEntity.builder()
                                .id(patientId)
                                .patientCode("BN" + System.currentTimeMillis())
                                .fullName("Nguyen Van A")
                                .dateOfBirth(java.time.LocalDate.of(1990, 1, 1))
                                .gender(com.benhsoan.domain.patient.enums.Gender.MALE)
                                .phone("0901234567")
                                .status(com.benhsoan.domain.patient.enums.PatientStatus.ACTIVE)
                                .createdAt(fixedNow)
                                .updatedAt(fixedNow)
                                .createdBy(UUID.randomUUID())
                                .build());

                UUID visitId = UUID.randomUUID();
                jpaVisitRepository.saveAndFlush(VisitEntity.builder()
                                .id(visitId)
                                .visitCode("KB" + System.currentTimeMillis())
                                .patientId(patientId)
                                .doctorId(UUID.randomUUID())
                                .visitType(VisitType.WALK_IN)
                                .status(VisitStatus.IN_PROGRESS)
                                .reason("Kham benh")
                                .visitAt(fixedNow)
                                .createdBy(UUID.randomUUID())
                                .createdAt(fixedNow)
                                .build());

                return visitId;
        }
}

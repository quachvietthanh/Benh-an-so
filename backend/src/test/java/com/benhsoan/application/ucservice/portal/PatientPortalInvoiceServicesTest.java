package com.benhsoan.application.ucservice.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.billing.InvoicePrintDocument;
import com.benhsoan.port.dto.result.billing.InvoicePrintDocument.InvoicePrintLine;
import com.benhsoan.port.dto.result.portal.InvoicePrintResult;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceDetailResult;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceDetailResult.InvoiceLineItemView;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceSummaryResult;
import com.benhsoan.port.outbound.pdf.InvoicePdfRenderer;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientPortalInvoiceServicesTest {

    private static final Instant NOW = Instant.parse("2026-09-21T08:30:00Z");

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock private InvoicePdfRenderer invoicePdfRenderer;
    @Mock private PatientAccessGuard patientAccessGuard;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private UUID patientId;
    private UUID visitId;
    private UUID doctorId;
    private UUID specialtyId;

    @BeforeEach
    void setUpCommon() {
        userId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        visitId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        specialtyId = UUID.randomUUID();
    }

    private Patient mockPatient(UUID pId, UUID uId) {
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(pId);
        when(patient.getPatientCode()).thenReturn("BN001");
        when(patient.getFullName()).thenReturn("Nguyễn Văn Bệnh Nhân");
        when(patient.getDateOfBirth()).thenReturn(LocalDate.of(1990, 5, 15));
        when(patient.getGender()).thenReturn(Gender.MALE);
        when(patient.getPhone()).thenReturn("0901234567");
        return patient;
    }

    private Visit mockVisit(UUID vId, UUID pId, UUID dId, UUID sId) {
        return Visit.restore(
                vId, "KB001", pId, dId, null, null, sId,
                VisitType.WALK_IN, VisitStatus.COMPLETED,
                NOW, NOW, NOW, "Kham benh", "Ghi chu",
                userId, NOW, NOW
        );
    }

    private Invoice mockInvoice(UUID invId, UUID vId, BigDecimal amount, InvoiceType type) {
        InvoiceLine line = InvoiceLine.create(
                UUID.randomUUID(), invId, InvoiceLineType.EXAM_FEE, "Phí khám bệnh",
                vId, 1, amount, amount, NOW
        );
        return Invoice.restore(
                invId, "HD-001", vId, UUID.randomUUID(), type,
                null, null, amount, userId, NOW,
                0, null, List.of(line)
        );
    }

    private Invoice mockInvoiceWithLines(UUID invId, UUID vId, List<InvoiceLine> lines, InvoiceType type) {
        BigDecimal total = lines.stream().map(InvoiceLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Invoice.restore(
                invId, "HD-001", vId, UUID.randomUUID(), type,
                null, null, total, userId, NOW,
                0, null, lines
        );
    }

    @Nested
    class ListInvoicesTests {

        private GetPatientPortalInvoicesService service;

        @BeforeEach
        void setUp() {
            service = new GetPatientPortalInvoicesService(
                    patientRepository, invoiceRepository, visitRepository,
                    userRepository, specialtyRepository, currentUserPort
            );
        }

        @Test
        void getInvoices_returnsInvoicesForCurrentPatient() {
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);
            Patient patient = mock(Patient.class);
            when(patient.getId()).thenReturn(patientId);
            when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

            UUID invId = UUID.randomUUID();
            Invoice invoice = mockInvoice(invId, visitId, new BigDecimal("150000"), InvoiceType.ORIGINAL);
            when(invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId)).thenReturn(List.of(invoice));

            Visit visit = mockVisit(visitId, patientId, doctorId, specialtyId);
            when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId)).thenReturn(List.of(visit));

            User doctor = mock(User.class);
            when(doctor.getId()).thenReturn(doctorId);
            when(doctor.getFullName()).thenReturn("BS. Trần Bác Sĩ");
            when(userRepository.findAllById(List.of(doctorId))).thenReturn(List.of(doctor));

            Specialty specialty = mock(Specialty.class);
            when(specialty.getId()).thenReturn(specialtyId);
            when(specialty.getName()).thenReturn("Khoa Nội");
            when(specialtyRepository.findAllById(List.of(specialtyId))).thenReturn(List.of(specialty));

            List<PatientPortalInvoiceSummaryResult> results = service.getInvoices(null);

            assertEquals(1, results.size());
            assertEquals(invId, results.get(0).invoiceId());
            assertEquals("HD-001", results.get(0).invoiceCode());
            assertEquals("BS. Trần Bác Sĩ", results.get(0).doctorName());
            assertEquals("Khoa Nội", results.get(0).specialtyName());
            assertEquals(1, results.get(0).itemCount());
        }

        @Test
        void getInvoices_whenNoInvoices_returnsEmptyList() {
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);
            Patient patient = mock(Patient.class);
            when(patient.getId()).thenReturn(patientId);
            when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
            when(invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId)).thenReturn(List.of());

            List<PatientPortalInvoiceSummaryResult> results = service.getInvoices(null);

            assertTrue(results.isEmpty());
        }

        @Test
        void getInvoices_whenNoPatientProfile_throwsAccessDeniedException() {
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);
            when(patientRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThrows(AccessDeniedException.class, () -> service.getInvoices(null));
        }

        @Test
        void getInvoices_relevantVisitsOptimization_onlyFetchesDoctorsForInvoiceVisits() {
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);
            Patient patient = mock(Patient.class);
            when(patient.getId()).thenReturn(patientId);
            when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

            UUID invId = UUID.randomUUID();
            Invoice invoice = mockInvoice(invId, visitId, new BigDecimal("100000"), InvoiceType.ORIGINAL);
            when(invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId)).thenReturn(List.of(invoice));

            UUID unrelatedVisitId = UUID.randomUUID();
            UUID unrelatedDoctorId = UUID.randomUUID();
            UUID unrelatedSpecialtyId = UUID.randomUUID();

            Visit relevantVisit = mockVisit(visitId, patientId, doctorId, specialtyId);
            Visit unrelatedVisit = mockVisit(unrelatedVisitId, patientId, unrelatedDoctorId, unrelatedSpecialtyId);
            when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId))
                    .thenReturn(List.of(relevantVisit, unrelatedVisit));

            User doctor = mock(User.class);
            when(doctor.getId()).thenReturn(doctorId);
            when(doctor.getFullName()).thenReturn("BS. Người Phụ Trách");
            when(userRepository.findAllById(List.of(doctorId))).thenReturn(List.of(doctor));

            Specialty specialty = mock(Specialty.class);
            when(specialty.getId()).thenReturn(specialtyId);
            when(specialty.getName()).thenReturn("Chuyên khoa liên quan");
            when(specialtyRepository.findAllById(List.of(specialtyId))).thenReturn(List.of(specialty));

            List<PatientPortalInvoiceSummaryResult> results = service.getInvoices(null, null);

            assertEquals(1, results.size());
            assertEquals("BS. Người Phụ Trách", results.get(0).doctorName());
            assertEquals("Chuyên khoa liên quan", results.get(0).specialtyName());

            // Assert that only relevant doctorId was queried, NOT the unrelated one
            verify(userRepository).findAllById(List.of(doctorId));
            verify(specialtyRepository).findAllById(List.of(specialtyId));
        }

        @Test
        void getInvoices_withLimit_respectsEffectiveLimit() {
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);
            Patient patient = mock(Patient.class);
            when(patient.getId()).thenReturn(patientId);
            when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

            Invoice inv1 = mockInvoice(UUID.randomUUID(), visitId, new BigDecimal("100000"), InvoiceType.ORIGINAL);
            Invoice inv2 = mockInvoice(UUID.randomUUID(), visitId, new BigDecimal("200000"), InvoiceType.ORIGINAL);
            Invoice inv3 = mockInvoice(UUID.randomUUID(), visitId, new BigDecimal("300000"), InvoiceType.ORIGINAL);
            when(invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId)).thenReturn(List.of(inv1, inv2, inv3));

            Visit visit = mockVisit(visitId, patientId, doctorId, specialtyId);
            when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId)).thenReturn(List.of(visit));

            List<PatientPortalInvoiceSummaryResult> results = service.getInvoices(null, 2);

            assertEquals(2, results.size());
        }

        @Test
        void getInvoices_whenDoctorOrSpecialtyNull_handlesNullSafely() {
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);
            Patient patient = mock(Patient.class);
            when(patient.getId()).thenReturn(patientId);
            when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

            UUID invId = UUID.randomUUID();
            Invoice invoice = mockInvoice(invId, visitId, new BigDecimal("150000"), InvoiceType.ORIGINAL);
            when(invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId)).thenReturn(List.of(invoice));

            // Visit mock with null doctor and null specialty
            Visit visitWithoutDoctorOrSpecialty = mock(Visit.class);
            when(visitWithoutDoctorOrSpecialty.getId()).thenReturn(visitId);
            when(visitWithoutDoctorOrSpecialty.getVisitCode()).thenReturn("KB-NO-DOC");
            when(visitWithoutDoctorOrSpecialty.getDoctorId()).thenReturn(null);
            when(visitWithoutDoctorOrSpecialty.getSpecialtyId()).thenReturn(null);
            when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId))
                    .thenReturn(List.of(visitWithoutDoctorOrSpecialty));

            List<PatientPortalInvoiceSummaryResult> results = service.getInvoices(null, null);

            assertEquals(1, results.size());
            assertEquals(invId, results.get(0).invoiceId());
            assertEquals(null, results.get(0).doctorName());
            assertEquals(null, results.get(0).specialtyName());
        }
    }

    @Nested
    class InvoiceDetailTests {

        private GetPatientPortalInvoiceDetailService service;

        @BeforeEach
        void setUp() {
            service = new GetPatientPortalInvoiceDetailService(
                    invoiceRepository, visitRepository, userRepository,
                    specialtyRepository, patientAccessGuard, auditLogRepository,
                    currentUserPort, clockPort, objectMapper
            );
        }

        @Test
        void getInvoiceDetail_returnsDetailWhenOwnedAndAuditsRead() {
            UUID invId = UUID.randomUUID();
            Invoice invoice = mockInvoice(invId, visitId, new BigDecimal("200000"), InvoiceType.ORIGINAL);
            Visit visit = mockVisit(visitId, patientId, doctorId, specialtyId);

            when(invoiceRepository.findById(invId)).thenReturn(Optional.of(invoice));
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
            when(clockPort.now()).thenReturn(NOW);
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);

            User doctor = mock(User.class);
            when(doctor.getFullName()).thenReturn("BS. Trần Bác Sĩ");
            when(userRepository.findById(any())).thenReturn(Optional.of(doctor));

            Specialty specialty = mock(Specialty.class);
            when(specialty.getName()).thenReturn("Khoa Nội");
            when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));

            PatientPortalInvoiceDetailResult result = service.getInvoiceDetail(invId);

            assertNotNull(result);
            assertEquals(invId, result.invoiceId());
            assertEquals("HD-001", result.invoiceCode());
            assertEquals(1, result.items().size());
            assertEquals("Phí khám bệnh", result.items().get(0).itemName());

            // Verify QTN-23 ownership check
            verify(patientAccessGuard).requirePatientOwnership(patientId, ResourceType.INVOICE, invId);

            // Verify READ audit log saved
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());
            assertEquals(ActionType.READ, auditCaptor.getValue().getActionType());
            assertEquals(ResourceType.INVOICE, auditCaptor.getValue().getResourceType());
            assertEquals(invId, auditCaptor.getValue().getResourceId());
        }

        @Test
        void getInvoiceDetail_whenAccessDenied_throwsException() {
            UUID invId = UUID.randomUUID();
            Invoice invoice = mockInvoice(invId, visitId, new BigDecimal("200000"), InvoiceType.ORIGINAL);
            Visit visit = mockVisit(visitId, patientId, doctorId, specialtyId);

            when(invoiceRepository.findById(invId)).thenReturn(Optional.of(invoice));
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
            when(patientAccessGuard.requirePatientOwnership(patientId, ResourceType.INVOICE, invId))
                    .thenThrow(new AccessDeniedException("Patient may only access their own data."));

            assertThrows(AccessDeniedException.class, () -> service.getInvoiceDetail(invId));
        }

        @Test
        void getInvoiceDetail_whenNotFound_throwsInvoiceNotFoundException() {
            UUID invId = UUID.randomUUID();
            when(invoiceRepository.findById(invId)).thenReturn(Optional.empty());

            assertThrows(InvoiceNotFoundException.class, () -> service.getInvoiceDetail(invId));
        }

        @Test
        void getInvoiceDetail_verifiesSumOfLineAmountsEqualsTotalAmount() {
            UUID invId = UUID.randomUUID();
            InvoiceLine line1 = InvoiceLine.create(UUID.randomUUID(), invId, InvoiceLineType.EXAM_FEE, "Khám chuyên khoa", visitId, 1, new BigDecimal("150000"), new BigDecimal("150000"), NOW);
            InvoiceLine line2 = InvoiceLine.create(UUID.randomUUID(), invId, InvoiceLineType.SERVICE_FEE, "Xét nghiệm máu", visitId, 1, new BigDecimal("100000"), new BigDecimal("100000"), NOW);
            Invoice invoice = mockInvoiceWithLines(invId, visitId, List.of(line1, line2), InvoiceType.ORIGINAL);
            Visit visit = mockVisit(visitId, patientId, doctorId, specialtyId);

            when(invoiceRepository.findById(invId)).thenReturn(Optional.of(invoice));
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
            when(clockPort.now()).thenReturn(NOW);
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);

            User doctor = mock(User.class);
            when(doctor.getFullName()).thenReturn("BS. Trần Bác Sĩ");
            when(userRepository.findById(any())).thenReturn(Optional.of(doctor));

            Specialty specialty = mock(Specialty.class);
            when(specialty.getName()).thenReturn("Khoa Nội");
            when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));

            PatientPortalInvoiceDetailResult result = service.getInvoiceDetail(invId);

            assertNotNull(result);
            assertEquals(2, result.items().size());
            BigDecimal linesSum = result.items().stream()
                    .map(InvoiceLineItemView::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, result.totalAmount().compareTo(linesSum),
                    "Invoice detail totalAmount must equal sum of line item amounts");
            assertEquals(0, new BigDecimal("250000").compareTo(result.totalAmount()));
        }
    }

    @Nested
    class ExportInvoiceTests {

        private ExportPatientPortalInvoiceService service;

        @BeforeEach
        void setUp() {
            service = new ExportPatientPortalInvoiceService(
                    invoiceRepository, visitRepository, patientRepository,
                    userRepository, specialtyRepository, clinicConfigurationRepository,
                    invoicePdfRenderer, patientAccessGuard, auditLogRepository,
                    currentUserPort, clockPort, objectMapper
            );
        }

        @Test
        void export_returnsPdfAndAuditsExportWhenOwned() {
            UUID invId = UUID.randomUUID();
            Invoice invoice = mockInvoice(invId, visitId, new BigDecimal("300000"), InvoiceType.ORIGINAL);
            Visit visit = mockVisit(visitId, patientId, doctorId, specialtyId);
            Patient patient = mockPatient(patientId, userId);

            when(invoiceRepository.findById(invId)).thenReturn(Optional.of(invoice));
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
            when(clockPort.now()).thenReturn(NOW);
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);

            byte[] fakePdf = new byte[]{1, 2, 3, 4};
            when(invoicePdfRenderer.render(any())).thenReturn(fakePdf);

            InvoicePrintResult result = service.export(invId);

            assertNotNull(result);
            assertEquals("hoa-don-HD-001.pdf", result.fileName());
            assertEquals("application/pdf", result.contentType());
            assertEquals(fakePdf, result.content());

            // Verify ownership check
            verify(patientAccessGuard).requirePatientOwnership(patientId, ResourceType.INVOICE, invId);

            // Verify EXPORT audit log saved
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());
            assertEquals(ActionType.EXPORT, auditCaptor.getValue().getActionType());
            assertEquals(ResourceType.INVOICE, auditCaptor.getValue().getResourceType());
        }

        @Test
        void export_whenNotOwner_throwsAccessDeniedException() {
            UUID invId = UUID.randomUUID();
            Invoice invoice = mockInvoice(invId, visitId, new BigDecimal("300000"), InvoiceType.ORIGINAL);
            Visit visit = mockVisit(visitId, patientId, doctorId, specialtyId);

            when(invoiceRepository.findById(invId)).thenReturn(Optional.of(invoice));
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
            when(patientAccessGuard.requirePatientOwnership(patientId, ResourceType.INVOICE, invId))
                    .thenThrow(new AccessDeniedException("Patient may only access their own data."));

            assertThrows(AccessDeniedException.class, () -> service.export(invId));
        }

        @Test
        void export_verifiesPrintDocumentLinesMatchInvoiceTotal() {
            UUID invId = UUID.randomUUID();
            InvoiceLine line1 = InvoiceLine.create(UUID.randomUUID(), invId, InvoiceLineType.EXAM_FEE, "Khám chuyên khoa", visitId, 1, new BigDecimal("200000"), new BigDecimal("200000"), NOW);
            InvoiceLine line2 = InvoiceLine.create(UUID.randomUUID(), invId, InvoiceLineType.SERVICE_FEE, "Siêu âm bụng", visitId, 1, new BigDecimal("150000"), new BigDecimal("150000"), NOW);
            Invoice invoice = mockInvoiceWithLines(invId, visitId, List.of(line1, line2), InvoiceType.ORIGINAL);
            Visit visit = mockVisit(visitId, patientId, doctorId, specialtyId);
            Patient patient = mockPatient(patientId, userId);

            when(invoiceRepository.findById(invId)).thenReturn(Optional.of(invoice));
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
            when(clockPort.now()).thenReturn(NOW);
            when(currentUserPort.getCurrentUserId()).thenReturn(userId);

            byte[] fakePdf = new byte[]{1, 2, 3, 4};
            ArgumentCaptor<InvoicePrintDocument> docCaptor = ArgumentCaptor.forClass(InvoicePrintDocument.class);
            when(invoicePdfRenderer.render(docCaptor.capture())).thenReturn(fakePdf);

            InvoicePrintResult result = service.export(invId);

            assertNotNull(result);
            InvoicePrintDocument capturedDoc = docCaptor.getValue();
            assertNotNull(capturedDoc);
            assertEquals(2, capturedDoc.lines().size());

            BigDecimal linesSum = capturedDoc.lines().stream()
                    .map(InvoicePrintLine::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, capturedDoc.totalAmount().compareTo(linesSum),
                    "InvoicePrintDocument totalAmount must equal sum of line item amounts");
            assertEquals(0, new BigDecimal("350000").compareTo(capturedDoc.totalAmount()));
        }
    }
}

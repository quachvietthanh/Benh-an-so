package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalInvoiceRestMapper;
import com.benhsoan.application.ucservice.patient.PatientAccessDeniedAuditWriter;
import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.application.ucservice.portal.ExportPatientPortalInvoiceService;
import com.benhsoan.application.ucservice.portal.GetPatientPortalInvoiceDetailService;
import com.benhsoan.application.ucservice.portal.GetPatientPortalInvoicesService;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.exception.GlobalExceptionHandler;
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

/**
 * Verifies QTN-23 data scoping and all Acceptance Criteria (TC-01 to TC-04) for NCL-14-CN-007.
 * Tests wire the actual services, real PatientAccessGuard, real PatientAccessDeniedAuditWriter
 * and GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientPortalInvoiceSecurityIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-21T08:30:00Z");

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock private InvoicePdfRenderer invoicePdfRenderer;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private MockMvc mockMvc;

    private UUID currentUserId;
    private UUID ownPatientId;
    private UUID otherPatientId;
    private UUID ownVisitId;
    private UUID otherVisitId;
    private UUID ownInvoiceId;
    private UUID otherInvoiceId;
    private UUID doctorId;
    private UUID specialtyId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        ownPatientId = UUID.randomUUID();
        otherPatientId = UUID.randomUUID();
        ownVisitId = UUID.randomUUID();
        otherVisitId = UUID.randomUUID();
        ownInvoiceId = UUID.randomUUID();
        otherInvoiceId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        specialtyId = UUID.randomUUID();

        ObjectMapper objectMapper = new ObjectMapper();
        PatientAccessDeniedAuditWriter denialAuditWriter =
                new PatientAccessDeniedAuditWriter(auditLogRepository, objectMapper);
        PatientAccessGuard patientAccessGuard =
                new PatientAccessGuard(currentUserPort, patientRepository, denialAuditWriter, clockPort);

        GetPatientPortalInvoicesService getInvoicesService = new GetPatientPortalInvoicesService(
                patientRepository, invoiceRepository, visitRepository,
                userRepository, specialtyRepository, currentUserPort
        );

        GetPatientPortalInvoiceDetailService getDetailService = new GetPatientPortalInvoiceDetailService(
                invoiceRepository, visitRepository, userRepository,
                specialtyRepository, patientAccessGuard, auditLogRepository,
                currentUserPort, clockPort, objectMapper
        );

        ExportPatientPortalInvoiceService exportService = new ExportPatientPortalInvoiceService(
                invoiceRepository, visitRepository, patientRepository,
                userRepository, specialtyRepository, clinicConfigurationRepository,
                invoicePdfRenderer, patientAccessGuard, auditLogRepository,
                currentUserPort, clockPort, objectMapper
        );

        PatientPortalInvoiceRestMapper mapper = new PatientPortalInvoiceRestMapper();

        PatientPortalInvoiceController controller = new PatientPortalInvoiceController(
                getInvoicesService, getDetailService, exportService, mapper
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Patient mockPatient(UUID id) {
        Patient patient = Mockito.mock(Patient.class);
        when(patient.getId()).thenReturn(id);
        when(patient.getPatientCode()).thenReturn("BN-001");
        when(patient.getFullName()).thenReturn("Nguyễn Văn Bệnh Nhân");
        when(patient.getGender()).thenReturn(Gender.MALE);
        return patient;
    }

    private Visit mockVisit(UUID vId, UUID pId) {
        return Visit.restore(
                vId, "KB-001", pId, doctorId, null, null, specialtyId,
                VisitType.WALK_IN, VisitStatus.COMPLETED,
                NOW, NOW, NOW, "Khám bệnh", "Ghi chú",
                currentUserId, NOW, NOW
        );
    }

    private Invoice mockInvoice(UUID invId, UUID vId, String code) {
        InvoiceLine line = InvoiceLine.create(
                UUID.randomUUID(), invId, InvoiceLineType.EXAM_FEE, "Khám chuyên khoa",
                vId, 1, new BigDecimal("150000"), new BigDecimal("150000"), NOW
        );
        return Invoice.restore(
                invId, code, vId, UUID.randomUUID(), InvoiceType.ORIGINAL,
                null, null, new BigDecimal("150000"), currentUserId, NOW,
                0, null, List.of(line)
        );
    }

    @Test
    void tc01_patientOpensInvoicesList_returnsOwnedInvoices() throws Exception {
        Patient ownPatient = mockPatient(ownPatientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(patientRepository.findByUserId(currentUserId)).thenReturn(Optional.of(ownPatient));

        Invoice invoice = mockInvoice(ownInvoiceId, ownVisitId, "HD-OWN-001");
        when(invoiceRepository.findByPatientIdOrderByCreatedAtDesc(ownPatientId)).thenReturn(List.of(invoice));

        Visit visit = mockVisit(ownVisitId, ownPatientId);
        when(visitRepository.findByPatientIdOrderByVisitAtDesc(ownPatientId)).thenReturn(List.of(visit));

        User doctor = Mockito.mock(User.class);
        when(doctor.getId()).thenReturn(doctorId);
        when(doctor.getFullName()).thenReturn("BS. Nguyễn Văn A");
        when(userRepository.findAllById(List.of(doctorId))).thenReturn(List.of(doctor));

        Specialty specialty = Mockito.mock(Specialty.class);
        when(specialty.getId()).thenReturn(specialtyId);
        when(specialty.getName()).thenReturn("Khoa Nội");
        when(specialtyRepository.findAllById(List.of(specialtyId))).thenReturn(List.of(specialty));

        mockMvc.perform(get("/patient-portal/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceId").value(ownInvoiceId.toString()))
                .andExpect(jsonPath("$[0].invoiceCode").value("HD-OWN-001"))
                .andExpect(jsonPath("$[0].doctorName").value("BS. Nguyễn Văn A"))
                .andExpect(jsonPath("$[0].specialtyName").value("Khoa Nội"));
    }

    @Test
    void tc02_patientDownloadsOwnInvoice_returnsPdfFile() throws Exception {
        Patient ownPatient = mockPatient(ownPatientId);
        Invoice invoice = mockInvoice(ownInvoiceId, ownVisitId, "HD-OWN-001");
        Visit visit = mockVisit(ownVisitId, ownPatientId);

        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(patientRepository.findByUserId(currentUserId)).thenReturn(Optional.of(ownPatient));
        when(invoiceRepository.findById(ownInvoiceId)).thenReturn(Optional.of(invoice));
        when(visitRepository.findById(ownVisitId)).thenReturn(Optional.of(visit));
        when(patientRepository.findById(ownPatientId)).thenReturn(Optional.of(ownPatient));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        byte[] renderedPdf = "%PDF-1.4 test invoice content".getBytes();
        when(invoicePdfRenderer.render(any())).thenReturn(renderedPdf);

        mockMvc.perform(get("/patient-portal/invoices/{invoiceId}/download", ownInvoiceId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hoa-don-HD-OWN-001.pdf\""))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/pdf")));

        // Verify audit log EXPORT is written
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(ActionType.EXPORT, captor.getValue().getActionType());
        assertEquals(ResourceType.INVOICE, captor.getValue().getResourceType());
        assertEquals(ownInvoiceId, captor.getValue().getResourceId());
    }

    @Test
    void tc03_patientAccessesOtherPatientsInvoice_returns403AndAuditsDenial() throws Exception {
        Patient ownPatient = mockPatient(ownPatientId);
        Invoice otherInvoice = mockInvoice(otherInvoiceId, otherVisitId, "HD-OTHER-999");
        Visit otherVisit = mockVisit(otherVisitId, otherPatientId);

        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(patientRepository.findByUserId(currentUserId)).thenReturn(Optional.of(ownPatient));
        when(invoiceRepository.findById(otherInvoiceId)).thenReturn(Optional.of(otherInvoice));
        when(visitRepository.findById(otherVisitId)).thenReturn(Optional.of(otherVisit));
        when(clockPort.now()).thenReturn(NOW);

        mockMvc.perform(get("/patient-portal/invoices/{invoiceId}", otherInvoiceId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // Verify audit log ACCESS_DENIED is written according to QTN-23
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(ActionType.ACCESS_DENIED, log.getActionType());
        assertEquals(ResourceType.INVOICE, log.getResourceType());
        assertEquals(otherInvoiceId, log.getResourceId());
        assertEquals(currentUserId, log.getUserId());
    }

    @Test
    void tc03_patientDownloadsOtherPatientsInvoice_returns403AndAuditsDenial() throws Exception {
        Patient ownPatient = mockPatient(ownPatientId);
        Invoice otherInvoice = mockInvoice(otherInvoiceId, otherVisitId, "HD-OTHER-999");
        Visit otherVisit = mockVisit(otherVisitId, otherPatientId);

        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(patientRepository.findByUserId(currentUserId)).thenReturn(Optional.of(ownPatient));
        when(invoiceRepository.findById(otherInvoiceId)).thenReturn(Optional.of(otherInvoice));
        when(visitRepository.findById(otherVisitId)).thenReturn(Optional.of(otherVisit));
        when(clockPort.now()).thenReturn(NOW);

        mockMvc.perform(get("/patient-portal/invoices/{invoiceId}/download", otherInvoiceId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // Verify denial audit
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(ActionType.ACCESS_DENIED, captor.getValue().getActionType());
        assertEquals(ResourceType.INVOICE, captor.getValue().getResourceType());
        assertEquals(otherInvoiceId, captor.getValue().getResourceId());
    }

    @Test
    void tc04_patientWithNoInvoices_returns200WithEmptyArray() throws Exception {
        Patient ownPatient = mockPatient(ownPatientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(patientRepository.findByUserId(currentUserId)).thenReturn(Optional.of(ownPatient));
        when(invoiceRepository.findByPatientIdOrderByCreatedAtDesc(ownPatientId)).thenReturn(List.of());

        mockMvc.perform(get("/patient-portal/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void itSec01_patientQueriesWithOtherPatientsVisitId_returnsEmptyArrayWithoutDataLeak() throws Exception {
        Patient ownPatient = mockPatient(ownPatientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(patientRepository.findByUserId(currentUserId)).thenReturn(Optional.of(ownPatient));

        // Patient has invoices, but none matching the foreign visitId
        Invoice ownInvoice = mockInvoice(ownInvoiceId, ownVisitId, "HD-OWN-001");
        when(invoiceRepository.findByPatientIdOrderByCreatedAtDesc(ownPatientId)).thenReturn(List.of(ownInvoice));

        // Query with otherVisitId
        mockMvc.perform(get("/patient-portal/invoices").param("visitId", otherVisitId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void tc01_patientViewsInvoiceDetail_itemsSumMatchesTotalAmount() throws Exception {
        Patient ownPatient = mockPatient(ownPatientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(patientRepository.findByUserId(currentUserId)).thenReturn(Optional.of(ownPatient));

        InvoiceLine line1 = InvoiceLine.create(
                UUID.randomUUID(), ownInvoiceId, InvoiceLineType.EXAM_FEE, "Khám chuyên khoa",
                ownVisitId, 1, new BigDecimal("150000"), new BigDecimal("150000"), NOW
        );
        InvoiceLine line2 = InvoiceLine.create(
                UUID.randomUUID(), ownInvoiceId, InvoiceLineType.SERVICE_FEE, "Xét nghiệm sinh hóa",
                ownVisitId, 1, new BigDecimal("200000"), new BigDecimal("200000"), NOW
        );
        Invoice invoice = Invoice.restore(
                ownInvoiceId, "HD-OWN-001", ownVisitId, UUID.randomUUID(), InvoiceType.ORIGINAL,
                null, null, new BigDecimal("350000"), currentUserId, NOW,
                0, null, List.of(line1, line2)
        );

        when(invoiceRepository.findById(ownInvoiceId)).thenReturn(Optional.of(invoice));

        Visit visit = mockVisit(ownVisitId, ownPatientId);
        when(visitRepository.findById(ownVisitId)).thenReturn(Optional.of(visit));

        User doctor = Mockito.mock(User.class);
        when(doctor.getFullName()).thenReturn("BS. Nguyễn Văn A");
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        Specialty specialty = Mockito.mock(Specialty.class);
        when(specialty.getName()).thenReturn("Khoa Nội");
        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));

        when(clockPort.now()).thenReturn(NOW);

        mockMvc.perform(get("/patient-portal/invoices/{invoiceId}", ownInvoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(ownInvoiceId.toString()))
                .andExpect(jsonPath("$.totalAmount").value(350000))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].amount").value(150000))
                .andExpect(jsonPath("$.items[1].amount").value(200000));
    }
}

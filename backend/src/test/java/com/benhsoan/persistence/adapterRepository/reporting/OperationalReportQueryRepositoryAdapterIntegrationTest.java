package com.benhsoan.persistence.adapterRepository.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.auth.UserEntity;
import com.benhsoan.persistence.entity.billing.InvoiceEntity;
import com.benhsoan.persistence.entity.medicine.MedicineEntity;
import com.benhsoan.persistence.entity.prescription.PrescriptionDispenseItemEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.billing.JpaInvoiceRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:reporting-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class OperationalReportQueryRepositoryAdapterIntegrationTest {

    @Autowired private JpaInvoiceRepository invoiceRepository;
    @Autowired private JpaVisitRepository visitRepository;
    @Autowired private EntityManager entityManager;

    private OperationalReportQueryRepositoryAdapter repositoryAdapter;

    @BeforeEach
    void setUp() {
        repositoryAdapter = new OperationalReportQueryRepositoryAdapter(entityManager);
    }

    @Test
    void countsCompletedVisitsInsideRangeOnly() {
        createVisit("VIS000001", VisitStatus.COMPLETED, Instant.parse("2026-08-01T02:00:00Z"));
        createVisit("VIS000002", VisitStatus.COMPLETED, Instant.parse("2026-08-03T04:00:00Z"));
        createVisit("VIS000003", VisitStatus.IN_PROGRESS, null);
        createVisit("VIS000004", VisitStatus.COMPLETED, Instant.parse("2026-08-04T00:00:00Z"));

        long count = repositoryAdapter.countCompletedVisits(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-04T00:00:00Z")
        );

        assertEquals(2L, count);
    }

    @Test
    void sumsNetRevenueUsingOriginalInvoicesAndNegativeAdjustments() {
        UUID visitId = UUID.randomUUID();
        createOriginalInvoice("HD000001", visitId, new BigDecimal("100000"), Instant.parse("2026-08-01T01:00:00Z"));
        createOriginalInvoice("HD000002", visitId, new BigDecimal("50000"), Instant.parse("2026-08-02T01:00:00Z"));
        createAdjustmentInvoice("HDDC000001", visitId, new BigDecimal("-20000"), Instant.parse("2026-08-03T01:00:00Z"));
        createAdjustmentInvoice("HDDC000002", visitId, new BigDecimal("-5000"), Instant.parse("2026-08-05T01:00:00Z"));

        BigDecimal revenue = repositoryAdapter.sumNetRevenue(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-04T00:00:00Z")
        );

        assertEquals(new BigDecimal("130000.00"), revenue);

        var timeline = repositoryAdapter.findDailyNetRevenue(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-04T00:00:00Z")
        );

        assertEquals(3, timeline.size());
        assertEquals(LocalDate.of(2026, 8, 1), timeline.get(0).date());
        assertEquals(new BigDecimal("100000.00"), timeline.get(0).revenue());
        assertEquals(LocalDate.of(2026, 8, 2), timeline.get(1).date());
        assertEquals(new BigDecimal("50000.00"), timeline.get(1).revenue());
        assertEquals(LocalDate.of(2026, 8, 3), timeline.get(2).date());
        assertEquals(new BigDecimal("-20000.00"), timeline.get(2).revenue());
    }

    @Test
    void aggregatesTopMedicinesByMedicineIdWithinDispensedAtRange() {
        UUID paracetamolId = UUID.fromString("16000000-0000-0000-0000-000000000001");
        UUID ibuprofenId = UUID.fromString("16000000-0000-0000-0000-000000000003");
        createMedicine(paracetamolId, "MED-PARA-500", "Paracetamol 500 mg");
        createMedicine(ibuprofenId, "MED-IBU-400", "Ibuprofen 400 mg");

        createDispenseItem(
                UUID.fromString("18300000-0000-0000-0000-000000000001"),
                paracetamolId,
                4,
                Instant.parse("2026-08-01T01:00:00Z"));
        createDispenseItem(
                UUID.fromString("18300000-0000-0000-0000-000000000002"),
                paracetamolId,
                5,
                Instant.parse("2026-08-02T01:00:00Z"));
        createDispenseItem(
                UUID.fromString("18300000-0000-0000-0000-000000000003"),
                ibuprofenId,
                10,
                Instant.parse("2026-08-02T03:00:00Z"));
        createDispenseItem(
                UUID.fromString("18300000-0000-0000-0000-000000000004"),
                ibuprofenId,
                3,
                Instant.parse("2026-08-05T03:00:00Z"));

        var items = repositoryAdapter.findTopDispensedMedicines(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-04T00:00:00Z")
        );

        assertEquals(2, items.size());
        assertEquals(ibuprofenId, items.get(0).medicineId());
        assertEquals("MED-IBU-400", items.get(0).medicineCode());
        assertEquals(10L, items.get(0).totalDispensedQuantity());
        assertEquals(paracetamolId, items.get(1).medicineId());
        assertEquals(9L, items.get(1).totalDispensedQuantity());
    }

    @Test
    void aggregatesDoctorVisitsByDoctorWithinCompletedAtRange() {
        UUID doctorA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
        UUID doctorB = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");
        UUID doctorC = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4");
        createDoctor(doctorA, "doctor1", "Dr. Nguyen Minh Anh");
        createDoctor(doctorB, "doctor2", "Dr. Tran Quang Huy");
        createDoctor(doctorC, "doctor3", "Dr. Le Van Chau");

        createCompletedVisitForDoctor("VIS-DOC-001", doctorA, Instant.parse("2026-08-01T02:00:00Z"));
        createCompletedVisitForDoctor("VIS-DOC-002", doctorA, Instant.parse("2026-08-02T02:00:00Z"));
        createCompletedVisitForDoctor("VIS-DOC-003", doctorB, Instant.parse("2026-08-01T03:00:00Z"));
        createCompletedVisitForDoctor("VIS-DOC-004", doctorB, Instant.parse("2026-08-02T03:00:00Z"));
        createCompletedVisitForDoctor("VIS-DOC-005", doctorB, Instant.parse("2026-08-03T03:00:00Z"));
        createCompletedVisitForDoctor("VIS-DOC-006", doctorC, Instant.parse("2026-08-01T04:00:00Z"));
        createCompletedVisitForDoctor("VIS-DOC-007", doctorC, Instant.parse("2026-08-02T04:00:00Z"));
        // Out of range / non-completed visits must be excluded.
        createCompletedVisitForDoctor("VIS-DOC-008", doctorB, Instant.parse("2026-08-04T00:00:00Z"));
        createVisit("VIS-DOC-009", VisitStatus.IN_PROGRESS, null);

        var items = repositoryAdapter.findDoctorVisitSummaries(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-04T00:00:00Z")
        );

        assertEquals(3, items.size());
        assertEquals(doctorB, items.get(0).doctorId());
        assertEquals("doctor2", items.get(0).doctorCode());
        assertEquals(3L, items.get(0).totalVisits());
        assertEquals(doctorC, items.get(1).doctorId());
        assertEquals(2L, items.get(1).totalVisits());
        assertEquals(doctorA, items.get(2).doctorId());
        assertEquals(2L, items.get(2).totalVisits());
    }

    private void createDoctor(UUID id, String username, String fullName) {
        entityManager.persist(UserEntity.builder()
                .id(id)
                .username(username)
                .passwordHash("hash")
                .fullName(fullName)
                .email(username + "@benhsoan.com")
                .roleId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .active(true)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build());
    }

    private void createCompletedVisitForDoctor(String code, UUID doctorId, Instant completedAt) {
        visitRepository.save(VisitEntity.builder()
                .id(UUID.randomUUID())
                .visitCode(code)
                .patientId(UUID.randomUUID())
                .doctorId(doctorId)
                .visitType(VisitType.WALK_IN)
                .status(VisitStatus.COMPLETED)
                .visitAt(Instant.parse("2026-08-01T00:00:00Z"))
                .startedAt(Instant.parse("2026-08-01T00:30:00Z"))
                .completedAt(completedAt)
                .reason("Kham tong quat")
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build());
    }

    private void createVisit(String code, VisitStatus status, Instant completedAt) {
        visitRepository.save(VisitEntity.builder()
                .id(UUID.randomUUID())
                .visitCode(code)
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .visitType(VisitType.WALK_IN)
                .status(status)
                .visitAt(Instant.parse("2026-08-01T00:00:00Z"))
                .startedAt(Instant.parse("2026-08-01T00:30:00Z"))
                .completedAt(completedAt)
                .reason("Kham tong quat")
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build());
    }

    private void createOriginalInvoice(String code, UUID visitId, BigDecimal amount, Instant createdAt) {
        invoiceRepository.save(InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .invoiceCode(code)
                .visitId(visitId)
                .paymentId(UUID.randomUUID())
                .type(InvoiceType.ORIGINAL)
                .totalAmount(amount)
                .createdBy(UUID.randomUUID())
                .createdAt(createdAt)
                .build());
    }

    private void createAdjustmentInvoice(String code, UUID visitId, BigDecimal amount, Instant createdAt) {
        invoiceRepository.save(InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .invoiceCode(code)
                .visitId(visitId)
                .type(InvoiceType.ADJUSTMENT)
                .originalInvoiceId(UUID.randomUUID())
                .adjustmentReason("Dieu chinh")
                .totalAmount(amount)
                .createdBy(UUID.randomUUID())
                .createdAt(createdAt)
                .build());
    }

    private void createMedicine(UUID id, String code, String name) {
        entityManager.persist(MedicineEntity.builder()
                .id(id)
                .medicineCode(code)
                .medicineName(name)
                .activeIngredient(name)
                .strength("500 mg")
                .dosageForm(DosageForm.TABLET)
                .unit("vien")
                .defaultRoute(AdministrationRoute.ORAL)
                .active(true)
                .stockQuantity(100)
                .minStockThreshold(10)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build());
    }

    private void createDispenseItem(UUID id, UUID medicineId, int quantity, Instant dispensedAt) {
        entityManager.persist(PrescriptionDispenseItemEntity.builder()
                .id(id)
                .prescriptionId(UUID.randomUUID())
                .prescriptionItemId(UUID.randomUUID())
                .medicineId(medicineId)
                .medicineBatchId(UUID.randomUUID())
                .dispensedQuantity(quantity)
                .dispensedBy(UUID.randomUUID())
                .dispensedAt(dispensedAt)
                .createdAt(dispensedAt)
                .build());
    }
}

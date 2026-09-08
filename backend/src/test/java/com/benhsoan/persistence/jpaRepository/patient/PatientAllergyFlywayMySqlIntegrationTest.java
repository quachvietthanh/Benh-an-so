package com.benhsoan.persistence.jpaRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.patient.exception.PatientAllergyAlreadyExistsException;
import com.benhsoan.port.dto.command.patient.AddPatientAllergyCommand;
import com.benhsoan.port.dto.command.patient.DeletePatientAllergyCommand;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;
import com.benhsoan.port.inbound.patient.AddPatientAllergyUseCase;
import com.benhsoan.port.inbound.patient.DeletePatientAllergyUseCase;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=WARN",
        "logging.level.org.hibernate.orm.jdbc.bind=WARN"
})
class PatientAllergyFlywayMySqlIntegrationTest {

    private static final UUID DOCTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID PATIENT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001");

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("patient_allergy_test")
            .withUsername("allergy_test")
            .withPassword("allergy_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private AddPatientAllergyUseCase addAllergyUseCase;
    @Autowired private DeletePatientAllergyUseCase deleteAllergyUseCase;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private CurrentUserPort currentUserPort;

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
    }

    @Test
    @DisplayName("MySQL V39: Unique constraint từ chối allergen trùng lặp trên cùng bệnh nhân")
    void uniqueConstraint_rejectsDuplicateActiveAllergen() {
        String allergen = "Penicillin-" + UUID.randomUUID().toString().substring(0, 6);

        AddPatientAllergyCommand cmd1 = AddPatientAllergyCommand.builder()
                .patientId(PATIENT_ID)
                .allergenType("MEDICATION")
                .allergenName(allergen)
                .severity(AllergySeverity.SEVERE)
                .reaction("Mề đay")
                .build();

        PatientAllergyResult first = addAllergyUseCase.addAllergy(cmd1);
        assertNotNull(first.id());

        AddPatientAllergyCommand cmd2 = AddPatientAllergyCommand.builder()
                .patientId(PATIENT_ID)
                .allergenType("MEDICATION")
                .allergenName(allergen)
                .severity(AllergySeverity.MILD)
                .build();

        assertThrows(PatientAllergyAlreadyExistsException.class, () -> addAllergyUseCase.addAllergy(cmd2));

        Integer activeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patient_allergies WHERE patient_id = UUID_TO_BIN(?) AND normalized_allergen_name = ? AND active = TRUE",
                Integer.class,
                PATIENT_ID.toString(),
                allergen.trim().toLowerCase()
        );
        assertEquals(1, activeCount);
    }

    @Test
    @DisplayName("MySQL V39: Unique constraint từ chối allergen biến thể chữ hoa/thường và khoảng trắng")
    void uniqueConstraint_rejectsCaseAndWhitespaceVariants() {
        String baseName = "Amoxicillin-" + UUID.randomUUID().toString().substring(0, 6);

        addAllergyUseCase.addAllergy(AddPatientAllergyCommand.builder()
                .patientId(PATIENT_ID)
                .allergenType("MEDICATION")
                .allergenName(baseName)
                .severity(AllergySeverity.MILD)
                .build());

        AddPatientAllergyCommand variantCmd = AddPatientAllergyCommand.builder()
                .patientId(PATIENT_ID)
                .allergenType("MEDICATION")
                .allergenName("  " + baseName.toUpperCase() + "  ")
                .severity(AllergySeverity.MODERATE)
                .build();

        assertThrows(PatientAllergyAlreadyExistsException.class, () -> addAllergyUseCase.addAllergy(variantCmd));
    }

    @Test
    @DisplayName("MySQL V39: Dị ứng đã xóa mềm (inactive) cho phép tạo lại cùng tên hoạt chất")
    void softDeletedAllergy_allowsRecreationOfSameAllergen() {
        String allergen = "Aspirin-" + UUID.randomUUID().toString().substring(0, 6);

        PatientAllergyResult created = addAllergyUseCase.addAllergy(AddPatientAllergyCommand.builder()
                .patientId(PATIENT_ID)
                .allergenType("MEDICATION")
                .allergenName(allergen)
                .severity(AllergySeverity.MILD)
                .build());

        // Soft delete (active -> false)
        deleteAllergyUseCase.deleteAllergy(DeletePatientAllergyCommand.builder()
                .patientId(PATIENT_ID)
                .allergyId(created.id())
                .reason("Chẩn đoán lại: Không còn dị ứng")
                .build());

        // Verify inactive in DB (active_normalized_name is NULL)
        Integer inactiveCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patient_allergies WHERE id = UUID_TO_BIN(?) AND active = FALSE AND active_normalized_name IS NULL",
                Integer.class,
                created.id().toString()
        );
        assertEquals(1, inactiveCount);

        // Tạo lại cùng hoạt chất -> Phải thành công vì MySQL unique index cho phép nhiều giá trị NULL
        PatientAllergyResult recreated = addAllergyUseCase.addAllergy(AddPatientAllergyCommand.builder()
                .patientId(PATIENT_ID)
                .allergenType("MEDICATION")
                .allergenName(allergen)
                .severity(AllergySeverity.MODERATE)
                .build());

        assertNotNull(recreated.id());
        assertFalse(recreated.id().equals(created.id()));

        Integer totalRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patient_allergies WHERE patient_id = UUID_TO_BIN(?) AND normalized_allergen_name = ?",
                Integer.class,
                PATIENT_ID.toString(),
                allergen.trim().toLowerCase()
        );
        assertEquals(2, totalRows);
    }

    @Test
    @DisplayName("MySQL V39: Concurrency duplicate insert -> Đúng 1 request thành công, 1 request nhận Conflict 409")
    void concurrentInsertSameAllergen_oneSucceedsOneReturnsConflict() throws Exception {
        String allergen = "Paracetamol-" + UUID.randomUUID().toString().substring(0, 6);

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger unexpectedExceptionCount = new AtomicInteger(0);

        Runnable task = () -> {
            readyLatch.countDown();
            try {
                startLatch.await(5, TimeUnit.SECONDS);
                addAllergyUseCase.addAllergy(AddPatientAllergyCommand.builder()
                        .patientId(PATIENT_ID)
                        .allergenType("MEDICATION")
                        .allergenName(allergen)
                        .severity(AllergySeverity.MILD)
                        .build());
                successCount.incrementAndGet();
            } catch (PatientAllergyAlreadyExistsException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                unexpectedExceptionCount.incrementAndGet();
            }
        };

        Future<?> f1 = executor.submit(task);
        Future<?> f2 = executor.submit(task);

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Đúng 1 request phải thành công");
        assertEquals(1, conflictCount.get(), "Request thứ hai phải ném PatientAllergyAlreadyExistsException (409)");
        assertEquals(0, unexpectedExceptionCount.get(), "Không được có lỗi không mong muốn (ví dụ 500)");

        Integer activeInDb = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patient_allergies WHERE patient_id = UUID_TO_BIN(?) AND normalized_allergen_name = ? AND active = TRUE",
                Integer.class,
                PATIENT_ID.toString(),
                allergen.trim().toLowerCase()
        );
        assertEquals(1, activeInDb);
    }
}

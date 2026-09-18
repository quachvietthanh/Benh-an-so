package com.benhsoan.application.ucservice.medicine;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.persistence.adapterRepository.medicine.MedicineRepositoryAdapter;
import com.benhsoan.persistence.mapper.medicine.MedicinePersistenceMapper;
import com.benhsoan.port.dto.command.medicine.CreateMedicineCommand;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        CreateMedicineService.class,
        MedicineRepositoryAdapter.class,
        MedicinePersistenceMapper.class,
        MedicineManagementAuthorizer.class,
        MedicineResultMapper.class
})
class AdminOperationAtomicityIntegrationTest {

    @Autowired
    private CreateMedicineService service;

    @Autowired
    private MedicineRepository medicineRepository;

    @MockitoBean
    private AdminOperationAuditService adminOperationAuditService;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private ClockPort clockPort;

    @Test
    void auditFailureRollsBackTheAdministrativeMutation() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        doThrow(new RuntimeException("audit persistence failed"))
                .when(adminOperationAuditService).record(any(), any(), any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class, () -> service.create(new CreateMedicineCommand(
                "MED-001", "Paracetamol", "Acetaminophen", "500 mg",
                DosageForm.TABLET, "vien", AdministrationRoute.ORAL, 20)));

        assertTrue(medicineRepository.findAll().isEmpty(),
                "Administrative mutation must roll back when the audit record cannot be written");
    }
}

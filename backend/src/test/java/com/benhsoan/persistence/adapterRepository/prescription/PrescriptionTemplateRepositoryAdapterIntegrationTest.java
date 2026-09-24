package com.benhsoan.persistence.adapterRepository.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.PrescriptionTemplate;
import com.benhsoan.domain.prescription.PrescriptionTemplateItem;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionTemplateRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionTemplatePersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionTemplateRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({PrescriptionTemplateRepositoryAdapter.class, PrescriptionTemplatePersistenceMapper.class})
class PrescriptionTemplateRepositoryAdapterIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-30T01:00:00Z");
    private static final UUID DIAGNOSIS_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();

    @Autowired
    private PrescriptionTemplateRepository repository;

    @Autowired
    private JpaPrescriptionTemplateRepository jpaRepository;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void savedTemplateIsReloadedWithOrderedItems() {
        UUID firstMedicine = UUID.randomUUID();
        UUID secondMedicine = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        PrescriptionTemplate saved = repository.save(PrescriptionTemplate.restore(
                templateId, DIAGNOSIS_ID, DOCTOR_ID, NOW,
                List.of(
                        item(templateId, UUID.randomUUID(), secondMedicine, 1),
                        item(templateId, UUID.randomUUID(), firstMedicine, 0)
                )
        ));

        PrescriptionTemplate reloaded = repository.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), reloaded.getId());
        assertEquals(DIAGNOSIS_ID, reloaded.getDiagnosisCatalogId());
        assertEquals(DOCTOR_ID, reloaded.getCreatedBy());
        assertEquals(2, reloaded.getItems().size());
        assertEquals(firstMedicine, reloaded.getItems().get(0).getMedicineId());
        assertEquals(secondMedicine, reloaded.getItems().get(1).getMedicineId());
    }

    @Test
    void findByDiagnosisCatalogIdAndCreatedByIsDoctorScoped() {
        repository.save(template(DIAGNOSIS_ID, DOCTOR_ID));
        repository.save(template(DIAGNOSIS_ID, UUID.randomUUID()));

        List<PrescriptionTemplate> mine = repository
                .findByDiagnosisCatalogIdAndCreatedBy(DIAGNOSIS_ID, DOCTOR_ID);

        assertEquals(1, mine.size());
        assertEquals(DOCTOR_ID, mine.get(0).getCreatedBy());
    }

    private PrescriptionTemplate template(UUID diagnosisId, UUID doctorId) {
        UUID templateId = UUID.randomUUID();
        return PrescriptionTemplate.restore(
                templateId, diagnosisId, doctorId, NOW,
                List.of(item(templateId, UUID.randomUUID(), UUID.randomUUID(), 0)));
    }

    private PrescriptionTemplateItem item(UUID templateId, UUID itemId, UUID medicineId, int sortOrder) {
        return PrescriptionTemplateItem.restore(
                itemId, templateId, medicineId, "1 viên", 2,
                AdministrationRoute.ORAL, 7, 14, null, sortOrder);
    }
}

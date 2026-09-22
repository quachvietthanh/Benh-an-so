package com.benhsoan.persistence.adapterRepository.controlledmedicine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.controlledmedicine.ControlledMedicineRegister;
import com.benhsoan.persistence.jpaRepository.controlledmedicine.JpaControlledMedicineRegisterRepository;
import com.benhsoan.persistence.mapper.controlledmedicine.ControlledMedicineRegisterPersistenceMapper;
import com.benhsoan.port.outbound.repository.controlledmedicine.ControlledMedicineRegisterRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ControlledMedicineRegisterRepositoryAdapter.class, ControlledMedicineRegisterPersistenceMapper.class})
class ControlledMedicineRegisterRepositoryAdapterIntegrationTest {

    @Autowired
    private ControlledMedicineRegisterRepository repository;

    @Autowired
    private JpaControlledMedicineRegisterRepository jpaRepository;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void savesAndSearchesRegisterByPatientAndMedicine() {
        UUID patientA = UUID.randomUUID();
        UUID patientB = UUID.randomUUID();
        UUID medicineX = UUID.randomUUID();
        UUID medicineY = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T10:00:00Z");

        repository.saveAll(List.of(
                record(patientA, medicineX, 5, now),
                record(patientA, medicineY, 2, now.plusSeconds(60)),
                record(patientB, medicineX, 3, now.plusSeconds(120))
        ));

        var byPatient = repository.search(
                new ControlledMedicineRegisterRepository.ControlledMedicineRegisterSearchCriteria(
                        patientA, null, null, null),
                PageRequest.of(0, 20));
        assertEquals(2, byPatient.getTotalElements());

        var byMedicine = repository.search(
                new ControlledMedicineRegisterRepository.ControlledMedicineRegisterSearchCriteria(
                        null, medicineX, null, null),
                PageRequest.of(0, 20));
        assertEquals(2, byMedicine.getTotalElements());
    }

    @Test
    void savedRecordIsReloadedUnchanged() {
        UUID patientId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T10:00:00Z");

        ControlledMedicineRegister saved = repository.save(record(patientId, medicineId, 7, now));

        var reloaded = repository.search(
                new ControlledMedicineRegisterRepository.ControlledMedicineRegisterSearchCriteria(
                        patientId, medicineId, null, null),
                PageRequest.of(0, 20)).getContent().getFirst();

        assertEquals(saved.getId(), reloaded.getId());
        assertEquals(saved.getPrescriptionId(), reloaded.getPrescriptionId());
        assertEquals(saved.getPrescribedBy(), reloaded.getPrescribedBy());
        assertEquals(saved.getDispensedBy(), reloaded.getDispensedBy());
        assertEquals(saved.getPatientId(), reloaded.getPatientId());
        assertEquals(saved.getMedicineId(), reloaded.getMedicineId());
        assertEquals(7, reloaded.getQuantity());
        assertEquals(now, reloaded.getDispensedAt());
    }

    @Test
    void repositoryPortExposesNoUpdateOrDeleteMethods() {
        for (Method method : ControlledMedicineRegisterRepository.class.getMethods()) {
            String name = method.getName().toLowerCase();
            assertFalse(name.startsWith("delete"), "Register must not expose delete: " + method.getName());
            assertFalse(name.startsWith("update"), "Register must not expose update: " + method.getName());
        }
    }

    private ControlledMedicineRegister record(UUID patientId, UUID medicineId, int quantity, Instant at) {
        return ControlledMedicineRegister.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                medicineId,
                "Morphine 10mg",
                patientId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                quantity,
                at
        );
    }
}

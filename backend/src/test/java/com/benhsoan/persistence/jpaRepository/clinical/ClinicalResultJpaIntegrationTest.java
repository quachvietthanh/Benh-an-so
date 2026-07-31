package com.benhsoan.persistence.jpaRepository.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.enums.MedicalAttachmentType;
import com.benhsoan.persistence.entity.clinical.ClinicalResultEntity;
import com.benhsoan.persistence.entity.clinical.ClinicalResultHistoryEntity;
import com.benhsoan.persistence.entity.clinical.MedicalAttachmentEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClinicalResultJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");

    @Autowired private JpaClinicalResultRepository clinicalResultRepository;
    @Autowired private JpaClinicalResultHistoryRepository clinicalResultHistoryRepository;
    @Autowired private JpaMedicalAttachmentRepository medicalAttachmentRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void commitsResultAttachmentAndHistoryTogether() {
        UUID visitId = UUID.randomUUID();
        ClinicalResultEntity result = result(UUID.randomUUID(), UUID.randomUUID(), visitId);

        inTransaction(() -> {
            clinicalResultRepository.save(result);
            medicalAttachmentRepository.save(attachment(result.getId(), visitId));
            clinicalResultHistoryRepository.save(history(result.getId()));
        });

        assertTrue(clinicalResultRepository.findById(result.getId()).isPresent());
        assertEquals(1, medicalAttachmentRepository.findByClinicalResultIdOrderByUploadedAtDesc(result.getId()).size());
        assertEquals(1, clinicalResultHistoryRepository.findByClinicalResultIdOrderByChangedAtDesc(result.getId()).size());
    }

    @Test
    void rollsBackResultAttachmentAndHistoryTogether() {
        UUID visitId = UUID.randomUUID();
        ClinicalResultEntity result = result(UUID.randomUUID(), UUID.randomUUID(), visitId);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            clinicalResultRepository.save(result);
            medicalAttachmentRepository.save(attachment(result.getId(), visitId));
            clinicalResultHistoryRepository.save(history(result.getId()));
            status.setRollbackOnly();
        });

        assertTrue(clinicalResultRepository.findById(result.getId()).isEmpty());
        assertTrue(medicalAttachmentRepository.findByClinicalResultIdOrderByUploadedAtDesc(result.getId()).isEmpty());
        assertTrue(clinicalResultHistoryRepository.findByClinicalResultIdOrderByChangedAtDesc(result.getId()).isEmpty());
    }

    @Test
    void enforcesOneResultPerClinicalOrderItem() {
        UUID clinicalOrderItemId = UUID.randomUUID();
        inTransaction(() -> clinicalResultRepository.saveAndFlush(result(UUID.randomUUID(), clinicalOrderItemId, UUID.randomUUID())));

        assertThrows(DataIntegrityViolationException.class,
                () -> inTransaction(() -> clinicalResultRepository.saveAndFlush(
                        result(UUID.randomUUID(), clinicalOrderItemId, UUID.randomUUID()))));
    }

    @Test
    void loadsAttachmentsForManyResultsWithOneBulkQueryMethod() {
        UUID visitId = UUID.randomUUID();
        ClinicalResultEntity first = result(UUID.randomUUID(), UUID.randomUUID(), visitId);
        ClinicalResultEntity second = result(UUID.randomUUID(), UUID.randomUUID(), visitId);
        inTransaction(() -> {
            clinicalResultRepository.saveAll(List.of(first, second));
            medicalAttachmentRepository.saveAll(List.of(attachment(first.getId(), visitId), attachment(second.getId(), visitId)));
        });

        assertEquals(2, medicalAttachmentRepository.findByClinicalResultIdIn(List.of(first.getId(), second.getId())).size());
    }

    private void inTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private ClinicalResultEntity result(UUID id, UUID itemId, UUID visitId) {
        return ClinicalResultEntity.builder().id(id).clinicalOrderItemId(itemId).visitId(visitId)
                .resultType(ClinicalResultType.NUMBER).numericValue(BigDecimal.TEN).unit("mmol/L")
                .abnormalFlag(ClinicalResultAbnormalFlag.NORMAL).status(ClinicalResultStatus.DRAFT)
                .enteredBy(UUID.randomUUID()).enteredAt(NOW).build();
    }

    private MedicalAttachmentEntity attachment(UUID resultId, UUID visitId) {
        return MedicalAttachmentEntity.builder().id(UUID.randomUUID()).visitId(visitId).clinicalResultId(resultId)
                .fileName("result.pdf").originalFileName("result.pdf").storageKey("mock/result.pdf")
                .contentType("application/pdf").fileSize(1024).attachmentType(MedicalAttachmentType.LAB_RESULT)
                .uploadedBy(UUID.randomUUID()).uploadedAt(NOW).build();
    }

    private ClinicalResultHistoryEntity history(UUID resultId) {
        return ClinicalResultHistoryEntity.builder().id(UUID.randomUUID()).clinicalResultId(resultId)
                .oldStatus(ClinicalResultStatus.DRAFT).newStatus(ClinicalResultStatus.FINAL)
                .changeReason("Finalized").changedBy(UUID.randomUUID()).changedAt(NOW).build();
    }
}

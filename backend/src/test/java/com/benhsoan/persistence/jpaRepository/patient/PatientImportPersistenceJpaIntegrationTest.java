package com.benhsoan.persistence.jpaRepository.patient;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.patient.enums.ImportStatus;
import com.benhsoan.persistence.entity.patient.PatientImportLogErrorEntity;
import com.benhsoan.persistence.entity.patient.PatientImportLogEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("Patient Import Persistence JPA Integration Tests")
class PatientImportPersistenceJpaIntegrationTest {

    @Autowired
    private JpaPatientImportLogRepository logRepository;

    @Autowired
    private JpaPatientImportLogErrorRepository errorRepository;

    @Test
    @DisplayName("Should save and retrieve import log with cascading errors")
    void shouldSaveAndRetrieveImportLogWithErrors() {
        UUID logId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        PatientImportLogEntity logEntity = PatientImportLogEntity.builder()
                .id(logId)
                .fileName("danh_sach_cu.xlsx")
                .fileSize(2048)
                .totalRows(10)
                .successRows(8)
                .errorRows(2)
                .duplicateRows(0)
                .status(ImportStatus.PARTIAL)
                .importedBy(userId)
                .createdAt(now)
                .build();

        PatientImportLogErrorEntity error1 = PatientImportLogErrorEntity.builder()
                .id(UUID.randomUUID())
                .importLog(logEntity)
                .rowNumber(3)
                .errorField("Họ và tên")
                .errorMessage("Họ và tên không được để trống")
                .rawData("raw row 3")
                .build();

        PatientImportLogErrorEntity error2 = PatientImportLogErrorEntity.builder()
                .id(UUID.randomUUID())
                .importLog(logEntity)
                .rowNumber(7)
                .errorField("Ngày sinh")
                .errorMessage("Ngày sinh không thể ở tương lai")
                .rawData("raw row 7")
                .build();

        logEntity.getErrors().add(error1);
        logEntity.getErrors().add(error2);

        logRepository.save(logEntity);

        var foundLog = logRepository.findById(logId);
        assertThat(foundLog).isPresent();
        assertThat(foundLog.get().getFileName()).isEqualTo("danh_sach_cu.xlsx");
        assertThat(foundLog.get().getStatus()).isEqualTo(ImportStatus.PARTIAL);
        assertThat(foundLog.get().getErrors()).hasSize(2);

        Page<PatientImportLogEntity> page = logRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);

        List<PatientImportLogErrorEntity> errorEntities = errorRepository.findByImportLogIdOrderByRowNumberAsc(logId);
        assertThat(errorEntities).hasSize(2);
        assertThat(errorEntities.get(0).getRowNumber()).isEqualTo(3);
        assertThat(errorEntities.get(1).getRowNumber()).isEqualTo(7);
    }
}

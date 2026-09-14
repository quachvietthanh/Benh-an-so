package com.benhsoan.adapter.inbound.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDetailResponse;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;

@DisplayName("MedicalRecordDetailRestMapper - Emergency Contact Tests (NCL-02-CN-007)")
class MedicalRecordDetailRestMapperTest {

    private AnonymizationModeState anonymizationModeState;
    private MedicalRecordDetailRestMapper mapper;

    @BeforeEach
    void setUp() {
        anonymizationModeState = new AnonymizationModeState();
        mapper = new MedicalRecordDetailRestMapper(anonymizationModeState);
    }

    @Test
    @DisplayName("TC-01: Hiển thị đầy đủ thông tin người liên hệ khẩn cấp ở đầu bệnh án khi không ẩn danh")
    void mapsEmergencyContactInPatientInfoSuccessfully() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        MedicalRecordDetailResult result = new MedicalRecordDetailResult(
                new MedicalRecordDetailResult.PatientInfo(
                        patientId, "BN001", "Nguyen Van A",
                        LocalDate.of(1990, 1, 1), Gender.MALE, "0901234567",
                        "079090001234", "DN4790123456789",
                        "Le Thi B", "Vợ", "0909998877"
                ),
                new MedicalRecordDetailResult.VisitInfo(
                        visitId, "VS-001", VisitType.WALK_IN, VisitStatus.COMPLETED,
                        Instant.now(), Instant.now(), Instant.now(),
                        "Kham benh", null, UUID.randomUUID(), "Dr. Tran"
                ),
                recordId, "Dau dau", "Sot", "Khong", "Binh thuong",
                "On dinh", "Nghi ngoi", "Tai kham", "On dinh",
                MedicalRecordStatus.OPEN, null, null, null, null, null,
                null, null, List.of(), List.of()
        );

        MedicalRecordDetailResponse response = mapper.toResponse(result);

        assertEquals("Le Thi B", response.patient().emergencyContact());
        assertEquals("Vợ", response.patient().emergencyRelationship());
        assertEquals("0909998877", response.patient().emergencyPhone());
    }

    @Test
    @DisplayName("QTN-43: Che dữ liệu người liên hệ khẩn cấp khi bật chế độ ẩn danh")
    void masksEmergencyContactWhenAnonymizationModeIsEnabled() {
        anonymizationModeState.setEnabled(true);

        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        MedicalRecordDetailResult result = new MedicalRecordDetailResult(
                new MedicalRecordDetailResult.PatientInfo(
                        patientId, "BN001", "Nguyen Van A",
                        LocalDate.of(1990, 1, 1), Gender.MALE, "0901234567",
                        "079090001234", "DN4790123456789",
                        "Le Thi B", "Vợ", "0909998877"
                ),
                new MedicalRecordDetailResult.VisitInfo(
                        visitId, "VS-001", VisitType.WALK_IN, VisitStatus.COMPLETED,
                        Instant.now(), Instant.now(), Instant.now(),
                        "Kham benh", null, UUID.randomUUID(), "Dr. Tran"
                ),
                recordId, "Dau dau", "Sot", "Khong", "Binh thuong",
                "On dinh", "Nghi ngoi", "Tai kham", "On dinh",
                MedicalRecordStatus.OPEN, null, null, null, null, null,
                null, null, List.of(), List.of()
        );

        MedicalRecordDetailResponse response = mapper.toResponse(result);

        assertEquals("BỆNH NHÂN", response.patient().emergencyContact());
        assertEquals("Vợ", response.patient().emergencyRelationship());
        assertEquals("09******77", response.patient().emergencyPhone());
    }

    @Test
    @DisplayName("Bệnh nhân không có người liên hệ khẩn cấp trả về null an toàn")
    void handlesNullEmergencyContactGracefully() {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        MedicalRecordDetailResult result = new MedicalRecordDetailResult(
                new MedicalRecordDetailResult.PatientInfo(
                        patientId, "BN001", "Nguyen Van A",
                        LocalDate.of(1990, 1, 1), Gender.MALE, "0901234567",
                        "079090001234", "DN4790123456789",
                        null, null, null
                ),
                new MedicalRecordDetailResult.VisitInfo(
                        visitId, "VS-001", VisitType.WALK_IN, VisitStatus.COMPLETED,
                        Instant.now(), Instant.now(), Instant.now(),
                        "Kham benh", null, UUID.randomUUID(), "Dr. Tran"
                ),
                recordId, "Dau dau", "Sot", "Khong", "Binh thuong",
                "On dinh", "Nghi ngoi", "Tai kham", "On dinh",
                MedicalRecordStatus.OPEN, null, null, null, null, null,
                null, null, List.of(), List.of()
        );

        MedicalRecordDetailResponse response = mapper.toResponse(result);

        assertNull(response.patient().emergencyContact());
        assertNull(response.patient().emergencyRelationship());
        assertNull(response.patient().emergencyPhone());
    }
}

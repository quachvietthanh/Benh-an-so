package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.benhsoan.adapter.inbound.rest.request.medicalrecord.ApplyMedicalRecordTemplateRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.CreateMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.ReplaceMedicalRecordDiagnosesRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.SignMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.CreatePrescriptionItemRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.CreatePrescriptionRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.CheckInWalkInRequest;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.infrastructure.authSecurity.CurrentUserPrincipal;
import com.benhsoan.persistence.entity.auth.RoleEntity;
import com.benhsoan.persistence.entity.auth.UserEntity;
import com.benhsoan.persistence.entity.inventory.MedicineBatchEntity;
import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordTemplateEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordTemplateSectionEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordTemplateVersionEntity;
import com.benhsoan.persistence.entity.medicine.MedicineEntity;
import com.benhsoan.persistence.entity.patient.PatientEntity;
import com.benhsoan.persistence.entity.prescription.PrescriptionEntity;
import com.benhsoan.persistence.entity.queue.DoctorRoomAssignmentEntity;
import com.benhsoan.persistence.entity.queue.QueueItemEntity;
import com.benhsoan.persistence.entity.queue.RoomEntity;
import com.benhsoan.persistence.entity.specialty.SpecialtyEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaRoleRepository;
import com.benhsoan.persistence.jpaRepository.auth.JpaUserRepository;
import com.benhsoan.persistence.jpaRepository.inventory.JpaMedicineBatchRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaDiagnosisCatalogRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordTemplateRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordTemplateSectionRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordTemplateVersionRepository;
import com.benhsoan.persistence.jpaRepository.medicine.JpaMedicineRepository;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaDoctorRoomAssignmentRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaMedicalQueueRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaQueueItemRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaRoomRepository;
import com.benhsoan.persistence.jpaRepository.specialty.JpaSpecialtyRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionCodeSequenceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:clinical_e2e_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Full Clinical Encounter Workflow End-to-End API Integration Test")
class FullClinicalEncounterWorkflowE2EIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private JpaUserRepository userRepository;
    @Autowired private JpaRoleRepository roleRepository;
    @Autowired private JpaSpecialtyRepository specialtyRepository;
    @Autowired private JpaRoomRepository roomRepository;
    @Autowired private JpaDoctorRoomAssignmentRepository doctorRoomAssignmentRepository;
    @Autowired private JpaPatientRepository patientRepository;
    @Autowired private JpaMedicalRecordTemplateRepository templateRepository;
    @Autowired private JpaMedicalRecordTemplateVersionRepository templateVersionRepository;
    @Autowired private JpaMedicalRecordTemplateSectionRepository templateSectionRepository;
    @Autowired private JpaDiagnosisCatalogRepository diagnosisCatalogRepository;
    @Autowired private JpaMedicineRepository medicineRepository;
    @Autowired private JpaMedicineBatchRepository medicineBatchRepository;
    @Autowired private JpaMedicalRecordRepository medicalRecordRepository;
    @Autowired private JpaVisitRepository visitRepository;
    @Autowired private JpaQueueItemRepository queueItemRepository;
    @Autowired private JpaMedicalQueueRepository medicalQueueRepository;
    @Autowired private JpaPrescriptionRepository prescriptionRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockitoBean private PrescriptionCodeSequenceRepository sequenceRepository;

    private UUID receptionistId;
    private UUID doctorId;
    private UUID pharmacistId;
    private UUID specialtyId;
    private UUID roomId;
    private UUID patientId;
    private UUID templateId;
    private UUID templateVersionId;
    private UUID diagnosisCatalogId;
    private UUID medicineId;
    private UUID medicineBatchId;

    private Authentication receptionistAuth;
    private Authentication doctorAuth;
    private Authentication pharmacistAuth;

    @BeforeEach
    void setUpTestData() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE prescription_dispense_items");
        jdbcTemplate.execute("TRUNCATE TABLE prescription_items");
        jdbcTemplate.execute("TRUNCATE TABLE prescriptions");
        jdbcTemplate.execute("TRUNCATE TABLE medical_record_diagnoses");
        jdbcTemplate.execute("TRUNCATE TABLE medical_record_access_logs");
        jdbcTemplate.execute("TRUNCATE TABLE medical_records");
        jdbcTemplate.execute("TRUNCATE TABLE queue_items");
        jdbcTemplate.execute("TRUNCATE TABLE medical_queues");
        jdbcTemplate.execute("TRUNCATE TABLE visits");
        jdbcTemplate.execute("TRUNCATE TABLE doctor_room_assignments");
        jdbcTemplate.execute("TRUNCATE TABLE rooms");
        jdbcTemplate.execute("TRUNCATE TABLE patients");
        jdbcTemplate.execute("TRUNCATE TABLE medical_record_template_sections");
        jdbcTemplate.execute("TRUNCATE TABLE medical_record_template_versions");
        jdbcTemplate.execute("TRUNCATE TABLE medical_record_templates");
        jdbcTemplate.execute("TRUNCATE TABLE diagnosis_catalog");
        jdbcTemplate.execute("TRUNCATE TABLE medicine_batches");
        jdbcTemplate.execute("TRUNCATE TABLE medicines");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("TRUNCATE TABLE roles");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        when(sequenceRepository.reserveNextValue(any())).thenReturn(1L);

        receptionistId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        pharmacistId = UUID.randomUUID();
        specialtyId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        templateVersionId = UUID.randomUUID();
        diagnosisCatalogId = UUID.randomUUID();
        medicineId = UUID.randomUUID();
        medicineBatchId = UUID.randomUUID();

        UUID roleReceptionistId = UUID.randomUUID();
        UUID roleDoctorId = UUID.randomUUID();
        UUID rolePharmacistId = UUID.randomUUID();

        roleRepository.save(RoleEntity.builder()
                .id(roleReceptionistId).name("RECEPTIONIST").description("Receptionist").isSystem(true)
                .createdAt(Instant.now()).build());
        roleRepository.save(RoleEntity.builder()
                .id(roleDoctorId).name("DOCTOR").description("Doctor").isSystem(true)
                .createdAt(Instant.now()).build());
        roleRepository.save(RoleEntity.builder()
                .id(rolePharmacistId).name("PHARMACIST").description("Pharmacist").isSystem(true)
                .createdAt(Instant.now()).build());

        userRepository.save(UserEntity.builder()
                .id(receptionistId).username("receptionist.test").passwordHash("hash")
                .fullName("Lễ Tân Test").email("receptionist@clinic.com")
                .roleId(roleReceptionistId).active(true).createdAt(Instant.now()).build());

        userRepository.save(UserEntity.builder()
                .id(doctorId).username("doctor.test").passwordHash("hash")
                .fullName("BS. Nguyễn Văn Khám").email("doctor@clinic.com")
                .roleId(roleDoctorId).active(true).createdAt(Instant.now()).build());

        userRepository.save(UserEntity.builder()
                .id(pharmacistId).username("pharmacist.test").passwordHash("hash")
                .fullName("Dược Sĩ Test").email("pharmacist@clinic.com")
                .roleId(rolePharmacistId).active(true).createdAt(Instant.now()).build());

        specialtyRepository.save(SpecialtyEntity.builder()
                .id(specialtyId).code("INTERNAL_MEDICINE").name("Nội Tổng Quát")
                .active(true).createdAt(Instant.now()).build());

        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("ROOM-101");
        room.setName("Phòng Khám Nội 101");
        room.setActive(true);
        room.setCreatedAt(Instant.now());
        roomRepository.save(room);

        DoctorRoomAssignmentEntity assignment = new DoctorRoomAssignmentEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setDoctorId(doctorId);
        assignment.setRoomId(roomId);
        assignment.setAssignedBy(receptionistId);
        assignment.setAssignedAt(Instant.now());
        doctorRoomAssignmentRepository.save(assignment);

        patientRepository.save(PatientEntity.builder()
                .id(patientId).patientCode("BN" + System.currentTimeMillis() % 100000)
                .fullName("Trần Thị Bệnh Nhân").dateOfBirth(LocalDate.of(1990, 5, 20))
                .gender(Gender.FEMALE).phone("0987654321").email("patient@gmail.com")
                .bloodType(BloodType.O_POSITIVE).active(true).createdBy(receptionistId)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build());

        templateRepository.save(MedicalRecordTemplateEntity.builder()
                .id(templateId).specialtyId(specialtyId).name("Mẫu Bệnh Án Nội Khoa")
                .nameKey("INTERNAL_DEFAULT").active(true).defaultTemplate(true)
                .currentVersionNo(1).createdBy(doctorId).createdAt(Instant.now()).build());

        templateVersionRepository.save(MedicalRecordTemplateVersionEntity.builder()
                .id(templateVersionId).templateId(templateId).versionNo(1)
                .specialtyId(specialtyId).templateName("Mẫu Bệnh Án Nội Khoa v1")
                .changeNote("Initial internal medicine template").createdBy(doctorId)
                .createdAt(Instant.now()).build());

        templateSectionRepository.save(MedicalRecordTemplateSectionEntity.builder()
                .id(UUID.randomUUID()).templateVersionId(templateVersionId)
                .fieldCode(MedicalRecordFieldCode.CHIEF_COMPLAINT).label("Lý do khám")
                .required(true).displayOrder(1).build());

        templateSectionRepository.save(MedicalRecordTemplateSectionEntity.builder()
                .id(UUID.randomUUID()).templateVersionId(templateVersionId)
                .fieldCode(MedicalRecordFieldCode.TREATMENT_PLAN).label("Hướng điều trị")
                .required(true).displayOrder(2).build());

        templateSectionRepository.save(MedicalRecordTemplateSectionEntity.builder()
                .id(UUID.randomUUID()).templateVersionId(templateVersionId)
                .fieldCode(MedicalRecordFieldCode.CONCLUSION).label("Kết luận")
                .required(true).displayOrder(3).build());

        diagnosisCatalogRepository.save(DiagnosisCatalogEntity.builder()
                .id(diagnosisCatalogId).code("J00").name("Viêm mũi họng cấp [cảm thường]")
                .diseaseGroup("Hô hấp").active(true).createdAt(Instant.now()).build());

        medicineRepository.save(MedicineEntity.builder()
                .id(medicineId).medicineCode("MED-PARA-500").medicineName("Paracetamol 500mg")
                .activeIngredient("Paracetamol").strength("500mg").dosageForm(DosageForm.TABLET)
                .unit("viên").defaultRoute(AdministrationRoute.ORAL).active(true)
                .createdAt(Instant.now()).build());

        medicineBatchRepository.save(MedicineBatchEntity.builder()
                .id(medicineBatchId).medicineId(medicineId).batchNumber("BATCH-2026-001")
                .expiryDate(LocalDate.now().plusMonths(12)).quantity(500)
                .status(BatchStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now()).build());

        receptionistAuth = createAuth(receptionistId, "receptionist.test", "RECEPTIONIST",
                "QUEUE_CREATE", "QUEUE_READ", "PATIENT_READ");
        doctorAuth = createAuth(doctorId, "doctor.test", "DOCTOR",
                "MEDICAL_RECORD_CREATE", "MEDICAL_RECORD_READ", "MEDICAL_RECORD_UPDATE",
                "MEDICAL_RECORD_UPDATE_STATUS", "PRESCRIPTION_CREATE", "PRESCRIPTION_READ",
                "PRESCRIPTION_UPDATE", "QUEUE_READ", "PATIENT_READ");
        pharmacistAuth = createAuth(pharmacistId, "pharmacist.test", "PHARMACIST",
                "PRESCRIPTION_READ", "PRESCRIPTION_UPDATE_STATUS");
    }

    private Authentication createAuth(UUID userId, String username, String role, String... permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        for (String perm : permissions) {
            authorities.add(new SimpleGrantedAuthority("PERMISSION_" + perm));
        }
        return new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(userId, username),
                null,
                authorities
        );
    }

    @Test
    @DisplayName("Thực thi toàn vẹn luồng lâm sàng thực tế: Đến khám -> Hàng đợi -> Gọi khám -> Chọn template & ghi bệnh án -> Kê đơn -> Ký & Khóa -> Cấp phát thuốc -> Hoàn thành")
    void executesFullClinicalEncounterWorkflowSuccessfully() throws Exception {
        // =========================================================================
        // BƯỚC 1: ĐẾN KHÁM (Check-in Walk-in tại quầy tiếp đón)
        // =========================================================================
        CheckInWalkInRequest walkInRequest = new CheckInWalkInRequest(
                patientId,
                doctorId,
                "Sốt cao 38.5 độ C, ho khan, đau họng",
                "Bệnh nhân tự đến khám buổi sáng",
                specialtyId
        );

        MvcResult checkInResult = mockMvc.perform(post("/queue-items/walk-in")
                        .with(authentication(receptionistAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walkInRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueItemId").isNotEmpty())
                .andExpect(jsonPath("$.visitId").isNotEmpty())
                .andExpect(jsonPath("$.queueNumber").isNumber())
                .andExpect(jsonPath("$.sourceType").value("WALK_IN"))
                .andReturn();

        JsonNode checkInJson = objectMapper.readTree(checkInResult.getResponse().getContentAsString());
        UUID queueItemId = UUID.fromString(checkInJson.get("queueItemId").asText());
        UUID visitId = UUID.fromString(checkInJson.get("visitId").asText());
        UUID queueId = UUID.fromString(checkInJson.get("medicalQueueId").asText());

        // Kiểm tra database: Visit và QueueItem đều ở trạng thái WAITING
        VisitEntity initialVisit = visitRepository.findById(visitId).orElseThrow();
        assertEquals(VisitStatus.WAITING, initialVisit.getStatus());
        QueueItemEntity initialQueueItem = queueItemRepository.findById(queueItemId).orElseThrow();
        assertEquals(QueueItemStatus.WAITING, initialQueueItem.getStatus());

        // =========================================================================
        // BƯỚC 2: HÀNG ĐỢI (Bác sĩ & Lễ tân theo dõi danh sách hàng đợi)
        // =========================================================================
        // 2.1. Bác sĩ xem hàng đợi phòng khám của chính mình (/queues/me)
        mockMvc.perform(get("/queues/me")
                        .with(authentication(doctorAuth))
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(queueItemId.toString()))
                .andExpect(jsonPath("$[0].status").value("WAITING"))
                .andExpect(jsonPath("$[0].patientName").value("Trần Thị Bệnh Nhân"));

        // 2.2. Lễ tân tra cứu danh sách hàng đợi tổng thể (/queues)
        mockMvc.perform(get("/queues")
                        .with(authentication(receptionistAuth))
                        .param("date", LocalDate.now().toString())
                        .param("doctorId", doctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(queueItemId.toString()));

        // =========================================================================
        // BƯỚC 3: GỌI KHÁM (Bác sĩ gọi bệnh nhân tiếp theo và bắt đầu lượt khám)
        // =========================================================================
        mockMvc.perform(post("/queues/{queueId}/call-next", queueId)
                        .with(authentication(doctorAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queueItemId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Kiểm tra database: QueueItem và Visit đã chuyển IN_PROGRESS
        VisitEntity inProgressVisit = visitRepository.findById(visitId).orElseThrow();
        assertEquals(VisitStatus.IN_PROGRESS, inProgressVisit.getStatus());
        QueueItemEntity inProgressQueueItem = queueItemRepository.findById(queueItemId).orElseThrow();
        assertEquals(QueueItemStatus.IN_PROGRESS, inProgressQueueItem.getStatus());

        // =========================================================================
        // BƯỚC 4: CHỌN TEMPLATE & GHI BỆNH ÁN
        // =========================================================================
        // 4.1. Bác sĩ tra cứu danh sách template khả dụng cho lượt khám
        mockMvc.perform(get("/medical-records/visits/{visitId}/template-options", visitId)
                        .with(authentication(doctorAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitSpecialty.id").value(specialtyId.toString()))
                .andExpect(jsonPath("$.availableTemplates[0].templateId").value(templateId.toString()))
                .andExpect(jsonPath("$.availableTemplates[0].templateVersionId").value(templateVersionId.toString()))
                .andExpect(jsonPath("$.effectiveTemplate.templateVersionId").value(templateVersionId.toString()));

        // 4.2. Bác sĩ khởi tạo hồ sơ bệnh án cho lượt khám (chưa có nội dung lâm sàng để chuẩn bị áp template)
        CreateMedicalRecordRequest createRecordRequest = new CreateMedicalRecordRequest(
                visitId,
                null,
                null, null, null, null, null, null, null
        );

        MvcResult createRecordResult = mockMvc.perform(post("/medical-records")
                        .with(authentication(doctorAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRecordRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        JsonNode recordJson = objectMapper.readTree(createRecordResult.getResponse().getContentAsString());
        UUID medicalRecordId = UUID.fromString(recordJson.get("id").asText());

        // 4.3. Bác sĩ áp template chuyên khoa vào bệnh án
        ApplyMedicalRecordTemplateRequest applyTemplateRequest = new ApplyMedicalRecordTemplateRequest(templateId);
        mockMvc.perform(put("/medical-records/{id}/template", medicalRecordId)
                        .with(authentication(doctorAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyTemplateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedTemplate").exists())
                .andExpect(jsonPath("$.appliedTemplate.templateVersionId").value(templateVersionId.toString()))
                .andExpect(jsonPath("$.appliedTemplate.name").value("Mẫu Bệnh Án Nội Khoa v1"));

        // 4.4. Bác sĩ nhập dữ liệu khám lâm sàng (đáp ứng các mục required của template: CHIEF_COMPLAINT, TREATMENT_PLAN, CONCLUSION)
        UpdateMedicalRecordRequest updateRecordRequest = new UpdateMedicalRecordRequest(
                "Sốt cao kèm ho khan 2 ngày",
                "Nhiệt độ 38.5, gai rét, nuốt đau",
                "Không có tiền sử dị ứng thuốc",
                "Họng đỏ rực, không có giả mạc, tim đều, phổi không rale",
                "Bệnh diễn tiến cấp tính",
                "Nghỉ ngơi, uống nhiều nước ấm, điều trị triệu chứng hạ sốt",
                "Tái khám sau 3 ngày nếu còn sốt cao",
                "Viêm họng cấp tính do virus"
        );

        mockMvc.perform(put("/medical-records/{id}", medicalRecordId)
                        .with(authentication(doctorAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRecordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chiefComplaint").value("Sốt cao kèm ho khan 2 ngày"))
                .andExpect(jsonPath("$.conclusion").value("Viêm họng cấp tính do virus"));

        // 4.5. Bác sĩ gắn mã chẩn đoán ICD-10 chính từ danh mục
        ReplaceMedicalRecordDiagnosesRequest diagnosesRequest = new ReplaceMedicalRecordDiagnosesRequest(
                new ReplaceMedicalRecordDiagnosesRequest.PrimaryDiagnosisRequest(diagnosisCatalogId, "Chẩn đoán chính: J00"),
                List.of()
        );

        mockMvc.perform(put("/medical-records/{id}/diagnoses", medicalRecordId)
                        .with(authentication(doctorAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diagnosesRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosisCode").value("J00"))
                .andExpect(jsonPath("$[0].diagnosisType").value("PRIMARY"));

        // =========================================================================
        // BƯỚC 5: KÊ ĐƠN THUỐC
        // =========================================================================
        CreatePrescriptionRequest prescriptionRequest = new CreatePrescriptionRequest(
                medicalRecordId,
                "Uống thuốc sau bữa ăn 15 phút",
                List.of(new CreatePrescriptionItemRequest(
                        medicineId,
                        "500mg",
                        2,
                        AdministrationRoute.ORAL,
                        5,
                        10,
                        "Mỗi lần 1 viên, cách nhau tối thiểu 4-6 giờ khi sốt > 38.5 độ C"
                )),
                null
        );

        MvcResult prescriptionResult = mockMvc.perform(post("/prescriptions")
                        .with(authentication(doctorAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prescriptionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.medicalRecordId").value(medicalRecordId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_DISPENSE"))
                .andExpect(jsonPath("$.prescriptionCode").isNotEmpty())
                .andExpect(jsonPath("$.items[0].medicineName").value("Paracetamol 500mg"))
                .andReturn();

        UUID prescriptionId = UUID.fromString(objectMapper.readTree(prescriptionResult.getResponse().getContentAsString()).get("id").asText());

        // =========================================================================
        // BƯỚC 6: KÝ BỆNH ÁN & KHÓA BỆNH ÁN
        // =========================================================================
        // 6.1. Bác sĩ ký điện tử xác nhận bệnh án
        SignMedicalRecordRequest signRequest = new SignMedicalRecordRequest("Bác sĩ Nguyễn Văn Khám ký số");
        mockMvc.perform(post("/medical-records/{id}/sign", medicalRecordId)
                        .with(authentication(doctorAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"))
                .andExpect(jsonPath("$.signedBy").value(doctorId.toString()))
                .andExpect(jsonPath("$.signedAt").isNotEmpty());

        // 6.2. Bác sĩ thực hiện khóa bệnh án (chuyển sang LOCKED để đảm bảo tính bất biến)
        mockMvc.perform(post("/medical-records/{id}/lock", medicalRecordId)
                        .with(authentication(doctorAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.lockedBy").value(doctorId.toString()))
                .andExpect(jsonPath("$.lockedAt").isNotEmpty());

        // Kiểm tra database: Bệnh án đã LOCKED
        MedicalRecordEntity lockedRecord = medicalRecordRepository.findById(medicalRecordId).orElseThrow();
        assertEquals(MedicalRecordStatus.LOCKED, lockedRecord.getStatus());
        assertNotNull(lockedRecord.getLockedAt());

        // BẢO VỆ TÍNH BẤT BIẾN: Cố tình sửa đổi bệnh án sau khi đã LOCKED sẽ bị từ chối với 409 Conflict
        mockMvc.perform(put("/medical-records/{id}", medicalRecordId)
                        .with(authentication(doctorAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRecordRequest)))
                .andExpect(status().isConflict());

        // =========================================================================
        // BƯỚC 7: CẤP PHÁT THUỐC (Dược sĩ cấp phát thuốc theo đơn tại quầy dược)
        // =========================================================================
        int stockBeforeDispense = medicineBatchRepository.findById(medicineBatchId).orElseThrow().getQuantity();

        mockMvc.perform(post("/prescriptions/{id}/dispense", prescriptionId)
                        .with(authentication(pharmacistAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescription.id").value(prescriptionId.toString()))
                .andExpect(jsonPath("$.prescription.status").value("DISPENSED"))
                .andExpect(jsonPath("$.dispensedBy").value(pharmacistId.toString()))
                .andExpect(jsonPath("$.allocations[0].dispensedQuantity").value(10));

        // Kiểm tra database: Tồn kho đã bị trừ đúng 10 viên
        int stockAfterDispense = medicineBatchRepository.findById(medicineBatchId).orElseThrow().getQuantity();
        assertEquals(stockBeforeDispense - 10, stockAfterDispense);
        PrescriptionEntity dispensedPrescription = prescriptionRepository.findById(prescriptionId).orElseThrow();
        assertEquals(PrescriptionStatus.DISPENSED, dispensedPrescription.getStatus());

        // =========================================================================
        // BƯỚC 8: HOÀN THÀNH LƯỢT KHÁM (Bác sĩ hoàn thành phiên khám trong hàng đợi)
        // =========================================================================
        mockMvc.perform(post("/queue-items/{itemId}/complete", queueItemId)
                        .with(authentication(doctorAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queueItemId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Kiểm tra trạng thái cuối cùng trong database
        QueueItemEntity finalQueueItem = queueItemRepository.findById(queueItemId).orElseThrow();
        assertEquals(QueueItemStatus.COMPLETED, finalQueueItem.getStatus());
        assertNotNull(finalQueueItem.getCompletedAt());

        VisitEntity finalVisit = visitRepository.findById(visitId).orElseThrow();
        assertEquals(VisitStatus.COMPLETED, finalVisit.getStatus());
        assertNotNull(finalVisit.getCompletedAt());
    }

    @Test
    @DisplayName("Bảo vệ nghiệp vụ hoàn thành: Lượt khám không thể hoàn thành nếu bệnh án chưa được khóa (LOCKED)")
    void cannotCompleteQueueItemIfMedicalRecordIsNotLocked() throws Exception {
        // Check-in
        CheckInWalkInRequest walkInRequest = new CheckInWalkInRequest(
                patientId, doctorId, "Khám tổng quát", "Check-in test", specialtyId
        );
        MvcResult checkInResult = mockMvc.perform(post("/queue-items/walk-in")
                        .with(authentication(receptionistAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walkInRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(checkInResult.getResponse().getContentAsString());
        UUID queueItemId = UUID.fromString(json.get("queueItemId").asText());
        UUID queueId = UUID.fromString(json.get("medicalQueueId").asText());
        UUID visitId = UUID.fromString(json.get("visitId").asText());

        // Gọi khám -> IN_PROGRESS
        mockMvc.perform(post("/queues/{queueId}/call-next", queueId)
                        .with(authentication(doctorAuth)))
                .andExpect(status().isOk());

        // Tạo bệnh án ở trạng thái OPEN (chưa ký, chưa khóa)
        CreateMedicalRecordRequest createRecordRequest = new CreateMedicalRecordRequest(
                visitId, "Lý do khám", null, null, null, null, null, null, null
        );
        mockMvc.perform(post("/medical-records")
                        .with(authentication(doctorAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRecordRequest)))
                .andExpect(status().isCreated());

        // Cố tình hoàn thành lượt khám khi bệnh án vẫn OPEN -> Phải bị từ chối 409 Conflict
        mockMvc.perform(post("/queue-items/{itemId}/complete", queueItemId)
                        .with(authentication(doctorAuth)))
                .andExpect(status().isConflict());

        // Xác nhận trạng thái vẫn giữ nguyên IN_PROGRESS
        assertEquals(QueueItemStatus.IN_PROGRESS, queueItemRepository.findById(queueItemId).orElseThrow().getStatus());
        assertEquals(VisitStatus.IN_PROGRESS, visitRepository.findById(visitId).orElseThrow().getStatus());
    }
}

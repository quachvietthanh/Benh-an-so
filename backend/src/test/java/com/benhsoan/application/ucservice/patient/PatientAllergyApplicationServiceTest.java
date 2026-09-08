package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.patient.PatientAllergyChangeLog;
import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.patient.exception.PatientAllergyAlreadyExistsException;
import com.benhsoan.domain.patient.exception.PatientAllergyNotFoundException;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.command.patient.AddPatientAllergyCommand;
import com.benhsoan.port.dto.command.patient.DeletePatientAllergyCommand;
import com.benhsoan.port.dto.command.patient.UpdatePatientAllergyCommand;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PatientAllergyApplicationServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PatientAllergyRepository patientAllergyRepository;
    @Mock private PatientAllergyChangeLogRepository changeLogRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private PatientAllResultMapper resultMapper;
    private PatientAllergyChangeDetailBuilder changeDetailBuilder;

    private AddPatientAllergyService addService;
    private UpdatePatientAllergyService updateService;
    private DeletePatientAllergyService deleteService;
    private GetPatientAllergiesService getService;
    private GetPatientAllergyChangeLogsService getChangeLogsService;

    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final Instant fixedNow = Instant.parse("2026-09-07T08:00:00Z");

    @BeforeEach
    void setUp() {
        resultMapper = new PatientAllResultMapper();
        changeDetailBuilder = new PatientAllergyChangeDetailBuilder(new ObjectMapper());

        addService = new AddPatientAllergyService(
                patientRepository, patientAllergyRepository, changeLogRepository,
                visitRepository, auditLogRepository, currentUserPort, clockPort,
                resultMapper, changeDetailBuilder
        );

        updateService = new UpdatePatientAllergyService(
                patientRepository, patientAllergyRepository, changeLogRepository, auditLogRepository,
                currentUserPort, clockPort, resultMapper, changeDetailBuilder
        );

        deleteService = new DeletePatientAllergyService(
                patientRepository, patientAllergyRepository, changeLogRepository, auditLogRepository,
                currentUserPort, clockPort, changeDetailBuilder
        );

        getService = new GetPatientAllergiesService(
                patientRepository, patientAllergyRepository, resultMapper
        );

        getChangeLogsService = new GetPatientAllergyChangeLogsService(
                patientRepository, patientAllergyRepository, changeLogRepository, resultMapper
        );
    }

    @Test
    @DisplayName("TC-01: Bác sĩ thêm một hoạt chất gây dị ứng thành công")
    void tc01_shouldAddAllergySuccessfully() {
        Patient mockPatient = org.mockito.Mockito.mock(Patient.class);
        when(mockPatient.isActive()).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);
        when(clockPort.now()).thenReturn(fixedNow);
        when(patientAllergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(patientId, "penicillin"))
                .thenReturn(false);
        when(patientAllergyRepository.save(any(PatientAllergy.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AddPatientAllergyCommand command = AddPatientAllergyCommand.builder()
                .patientId(patientId)
                .allergenType("MEDICATION")
                .allergenName("Penicillin")
                .severity(AllergySeverity.SEVERE)
                .reaction("Nổi mề đay, khó thở nhẹ")
                .notes("Xảy ra năm 2022")
                .build();

        PatientAllergyResult result = addService.addAllergy(command);

        assertNotNull(result);
        assertEquals("Penicillin", result.allergenName());
        assertEquals(AllergySeverity.SEVERE, result.severity());
        assertEquals(doctorId, result.createdBy());

        // Kiểm tra lưu change log (TC-04)
        ArgumentCaptor<PatientAllergyChangeLog> logCaptor = ArgumentCaptor.forClass(PatientAllergyChangeLog.class);
        verify(changeLogRepository).save(logCaptor.capture());
        assertEquals("CREATE", logCaptor.getValue().getAction());
        assertTrue(logCaptor.getValue().getAfterData().contains("Penicillin"));

        // Kiểm tra lưu audit log (QTN-02)
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals(ActionType.CREATE, auditCaptor.getValue().getActionType());
        assertEquals(ResourceType.PATIENT_ALLERGY, auditCaptor.getValue().getResourceType());
        assertEquals(result.id(), auditCaptor.getValue().getResourceId());
        assertTrue(auditCaptor.getValue().getDetail().contains(patientId.toString()));
    }

    @Test
    @DisplayName("TC-02: Thêm lại đúng hoạt chất đã có -> Hệ thống báo trùng và không tạo thêm bản ghi")
    void tc02_shouldThrowWhenAllergenAlreadyExists() {
        Patient mockPatient = org.mockito.Mockito.mock(Patient.class);
        when(mockPatient.isActive()).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));
        when(patientAllergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(patientId, "aspirin"))
                .thenReturn(true);

        AddPatientAllergyCommand command = AddPatientAllergyCommand.builder()
                .patientId(patientId)
                .allergenName("Aspirin")
                .severity(AllergySeverity.MILD)
                .build();

        assertThrows(PatientAllergyAlreadyExistsException.class, () -> addService.addAllergy(command));
    }

    @Test
    @DisplayName("TC-04: Bác sĩ sửa một mục dị ứng -> Hệ thống lưu snapshot trước và sau khi sửa")
    void tc04_shouldRecordChangeLogWhenUpdatingAllergy() {
        UUID allergyId = UUID.randomUUID();
        Patient mockPatient = org.mockito.Mockito.mock(Patient.class);
        when(mockPatient.isActive()).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));

        PatientAllergy existingAllergy = PatientAllergy.create(
                patientId, "MEDICATION", "Penicillin", AllergySeverity.MILD,
                "Ngứa nhẹ", "Ghi nhận cũ", doctorId, fixedNow
        );

        when(patientAllergyRepository.findById(allergyId)).thenReturn(Optional.of(existingAllergy));
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);
        when(clockPort.now()).thenReturn(fixedNow.plusSeconds(300));
        when(patientAllergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrueAndIdNot(
                patientId, "penicillin g", allergyId)).thenReturn(false);
        when(patientAllergyRepository.save(any(PatientAllergy.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdatePatientAllergyCommand command = UpdatePatientAllergyCommand.builder()
                .allergyId(allergyId)
                .patientId(patientId)
                .allergenName("Penicillin G")
                .severity(AllergySeverity.SEVERE)
                .reaction("Khó thở cấp")
                .notes("Cập nhật theo xét nghiệm")
                .changeReason("Bổ sung diễn tiến nặng")
                .build();

        PatientAllergyResult result = updateService.updateAllergy(command);

        assertEquals("Penicillin G", result.allergenName());
        assertEquals(AllergySeverity.SEVERE, result.severity());

        // Verify snapshot trước và sau khi sửa
        ArgumentCaptor<PatientAllergyChangeLog> logCaptor = ArgumentCaptor.forClass(PatientAllergyChangeLog.class);
        verify(changeLogRepository).save(logCaptor.capture());
        PatientAllergyChangeLog log = logCaptor.getValue();
        assertEquals("UPDATE", log.getAction());
        assertTrue(log.getBeforeData().contains("Penicillin"));
        assertTrue(log.getBeforeData().contains("MILD"));
        assertTrue(log.getAfterData().contains("Penicillin G"));
        assertTrue(log.getAfterData().contains("SEVERE"));
        assertEquals("Bổ sung diễn tiến nặng", log.getChangeReason());
    }

    @Test
    @DisplayName("TC-04: Bác sĩ xóa một mục dị ứng -> Hệ thống vô hiệu hóa và lưu vết nội dung trước khi xóa")
    void tc04_shouldRecordChangeLogWhenDeletingAllergy() {
        UUID allergyId = UUID.randomUUID();
        Patient mockPatient = org.mockito.Mockito.mock(Patient.class);
        when(mockPatient.isActive()).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));

        PatientAllergy existingAllergy = PatientAllergy.create(
                patientId, "MEDICATION", "Paracetamol", AllergySeverity.MODERATE,
                "Ban đỏ", "Ghi chú", doctorId, fixedNow
        );

        when(patientAllergyRepository.findById(allergyId)).thenReturn(Optional.of(existingAllergy));
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);
        when(clockPort.now()).thenReturn(fixedNow.plusSeconds(600));

        DeletePatientAllergyCommand command = DeletePatientAllergyCommand.builder()
                .allergyId(allergyId)
                .patientId(patientId)
                .reason("Chẩn đoán lại: Không dị ứng thuốc này")
                .build();

        deleteService.deleteAllergy(command);

        assertFalse(existingAllergy.isActive());

        // Verify snapshot trước khi xóa
        ArgumentCaptor<PatientAllergyChangeLog> logCaptor = ArgumentCaptor.forClass(PatientAllergyChangeLog.class);
        verify(changeLogRepository).save(logCaptor.capture());
        PatientAllergyChangeLog log = logCaptor.getValue();
        assertEquals("DELETE", log.getAction());
        assertTrue(log.getBeforeData().contains("Paracetamol"));
        assertEquals("Chẩn đoán lại: Không dị ứng thuốc này", log.getChangeReason());
    }

    @Test
    @DisplayName("Báo lỗi khi lượt khám không thuộc về bệnh nhân")
    void shouldThrowWhenVisitDoesNotBelongToPatient() {
        UUID visitId = UUID.randomUUID();
        UUID anotherPatientId = UUID.randomUUID();

        Patient mockPatient = org.mockito.Mockito.mock(Patient.class);
        when(mockPatient.isActive()).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));

        Visit mockVisit = org.mockito.Mockito.mock(Visit.class);
        when(mockVisit.getPatientId()).thenReturn(anotherPatientId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(mockVisit));

        AddPatientAllergyCommand command = AddPatientAllergyCommand.builder()
                .patientId(patientId)
                .allergenName("Cefuroxime")
                .severity(AllergySeverity.MILD)
                .visitId(visitId)
                .build();

        assertThrows(ValidationException.class, () -> addService.addAllergy(command));
    }

    @Test
    @DisplayName("Tra cứu danh sách dị ứng của bệnh nhân")
    void shouldGetPatientAllergies() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(org.mockito.Mockito.mock(Patient.class)));
        PatientAllergy allergy = PatientAllergy.create(patientId, "MEDICATION", "Amoxicillin", AllergySeverity.MILD, null, null, doctorId, fixedNow);
        when(patientAllergyRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of(allergy));

        List<PatientAllergyResult> list = getService.getAllergies(patientId);

        assertEquals(1, list.size());
        assertEquals("Amoxicillin", list.get(0).allergenName());
    }

    @Test
    @DisplayName("TC-UC-01: Tra cứu lịch sử khi Bệnh nhân không tồn tại -> PatientNotFoundException")
    void tcUc01_getChangeLogs_whenPatientNotFound_shouldThrow() {
        UUID allergyId = UUID.randomUUID();
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> getChangeLogsService.getChangeLogs(patientId, allergyId));
    }

    @Test
    @DisplayName("TC-UC-02: Tra cứu lịch sử khi Mục dị ứng không thuộc Bệnh nhân -> PatientAllergyNotFoundException")
    void tcUc02_getChangeLogs_whenAllergyNotBelongToPatient_shouldThrow() {
        UUID allergyId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(org.mockito.Mockito.mock(Patient.class)));
        PatientAllergy allergyBelongingToOther = PatientAllergy.create(
                otherPatientId, "MEDICATION", "Aspirin", AllergySeverity.MILD, null, null, doctorId, fixedNow
        );
        when(patientAllergyRepository.findById(allergyId)).thenReturn(Optional.of(allergyBelongingToOther));

        assertThrows(PatientAllergyNotFoundException.class, () -> getChangeLogsService.getChangeLogs(patientId, allergyId));
    }

    @Test
    @DisplayName("TC-UC-03: Cập nhật mục dị ứng khi Bệnh nhân bị vô hiệu hóa -> PatientInactiveException")
    void tcUc03_updateAllergy_whenPatientInactive_shouldThrow() {
        Patient inactivePatient = org.mockito.Mockito.mock(Patient.class);
        when(inactivePatient.isActive()).thenReturn(false);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(inactivePatient));

        UpdatePatientAllergyCommand command = UpdatePatientAllergyCommand.builder()
                .allergyId(UUID.randomUUID())
                .patientId(patientId)
                .allergenName("Aspirin")
                .severity(AllergySeverity.MILD)
                .build();

        assertThrows(PatientInactiveException.class, () -> updateService.updateAllergy(command));
    }

    @Test
    @DisplayName("TC-UC-04: Xóa mục dị ứng khi Bệnh nhân bị vô hiệu hóa -> PatientInactiveException")
    void tcUc04_deleteAllergy_whenPatientInactive_shouldThrow() {
        Patient inactivePatient = org.mockito.Mockito.mock(Patient.class);
        when(inactivePatient.isActive()).thenReturn(false);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(inactivePatient));

        DeletePatientAllergyCommand command = DeletePatientAllergyCommand.builder()
                .allergyId(UUID.randomUUID())
                .patientId(patientId)
                .build();

        assertThrows(PatientInactiveException.class, () -> deleteService.deleteAllergy(command));
    }

    @Test
    @DisplayName("TC-UC-05: ID null khi gọi Get -> ValidationException")
    void tcUc05_getWithNullIds_shouldThrowValidationException() {
        assertThrows(ValidationException.class, () -> getService.getAllergies(null));
        assertThrows(ValidationException.class, () -> getChangeLogsService.getChangeLogs(null, UUID.randomUUID()));
        assertThrows(ValidationException.class, () -> getChangeLogsService.getChangeLogs(patientId, null));
    }

    @Test
    @DisplayName("TC-UC-06: Thêm dị ứng gắn với lượt khám đã kết thúc/hủy -> ValidationException")
    void tcUc06_addAllergy_whenVisitInactive_shouldThrowValidationException() {
        UUID visitId = UUID.randomUUID();
        Patient mockPatient = org.mockito.Mockito.mock(Patient.class);
        when(mockPatient.isActive()).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));

        Visit mockVisit = org.mockito.Mockito.mock(Visit.class);
        when(mockVisit.getPatientId()).thenReturn(patientId);
        when(mockVisit.isActive()).thenReturn(false); // Lượt khám đã kết thúc hoặc hủy
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(mockVisit));

        AddPatientAllergyCommand command = AddPatientAllergyCommand.builder()
                .patientId(patientId)
                .allergenName("Paracetamol")
                .severity(AllergySeverity.MILD)
                .visitId(visitId)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> addService.addAllergy(command));
        assertTrue(ex.getMessage().contains("Lượt khám đã kết thúc hoặc không còn hiệu lực"));
    }

    @Test
    @DisplayName("TC-UC-07: Bắt DataIntegrityViolationException khi lưu và ném PatientAllergyAlreadyExistsException")
    void tcUc07_concurrencyDuplicate_shouldThrowPatientAllergyAlreadyExistsException() {
        Patient mockPatient = org.mockito.Mockito.mock(Patient.class);
        when(mockPatient.isActive()).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);
        when(patientAllergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(any(), any())).thenReturn(false);
        when(patientAllergyRepository.save(any(PatientAllergy.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry uk_patient_active_allergen"));

        AddPatientAllergyCommand command = AddPatientAllergyCommand.builder()
                .patientId(patientId)
                .allergenName("Amoxicillin")
                .severity(AllergySeverity.MILD)
                .build();

        assertThrows(PatientAllergyAlreadyExistsException.class, () -> addService.addAllergy(command));
    }

    @Test
    @DisplayName("TC-UC-08: Cập nhật dị ứng thuộc bệnh nhân khác -> PatientAllergyNotFoundException (IDOR)")
    void tcUc08_updateAllergy_whenAllergyBelongsToOtherPatient_shouldThrowNotFoundAndNeverSave() {
        UUID allergyId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();

        Patient mockPatient = org.mockito.Mockito.mock(Patient.class);
        when(mockPatient.isActive()).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));

        PatientAllergy allergyBelongingToOther = PatientAllergy.create(
                otherPatientId, "MEDICATION", "Aspirin", AllergySeverity.MILD, null, null, doctorId, fixedNow
        );
        when(patientAllergyRepository.findById(allergyId)).thenReturn(Optional.of(allergyBelongingToOther));

        UpdatePatientAllergyCommand command = UpdatePatientAllergyCommand.builder()
                .allergyId(allergyId)
                .patientId(patientId)
                .allergenName("Aspirin Modified")
                .severity(AllergySeverity.MODERATE)
                .build();

        assertThrows(PatientAllergyNotFoundException.class, () -> updateService.updateAllergy(command));
        verify(patientAllergyRepository, never()).save(any());
        verify(changeLogRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-UC-09: Xóa dị ứng thuộc bệnh nhân khác -> PatientAllergyNotFoundException (IDOR)")
    void tcUc09_deleteAllergy_whenAllergyBelongsToOtherPatient_shouldThrowNotFoundAndNeverSave() {
        UUID allergyId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();

        Patient mockPatient = org.mockito.Mockito.mock(Patient.class);
        when(mockPatient.isActive()).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));

        PatientAllergy allergyBelongingToOther = PatientAllergy.create(
                otherPatientId, "MEDICATION", "Paracetamol", AllergySeverity.MODERATE, null, null, doctorId, fixedNow
        );
        when(patientAllergyRepository.findById(allergyId)).thenReturn(Optional.of(allergyBelongingToOther));

        DeletePatientAllergyCommand command = DeletePatientAllergyCommand.builder()
                .allergyId(allergyId)
                .patientId(patientId)
                .reason("Xóa nhầm")
                .build();

        assertThrows(PatientAllergyNotFoundException.class, () -> deleteService.deleteAllergy(command));
        assertTrue(allergyBelongingToOther.isActive());
        verify(patientAllergyRepository, never()).save(any());
        verify(changeLogRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }
}

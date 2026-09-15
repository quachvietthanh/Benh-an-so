package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

@ExtendWith(MockitoExtension.class)
class AppointmentResultAssemblerTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentRescheduleHistoryAssembler historyAssembler;

    @Captor
    private ArgumentCaptor<Set<UUID>> patientIdsCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> userIdsCaptor;

    private AppointmentResultMapper resultMapper;
    private AppointmentResultAssembler assembler;

    private final Instant now = Instant.parse("2026-09-14T08:00:00Z");

    @BeforeEach
    void setUp() {
        resultMapper = new AppointmentResultMapper();
        assembler = new AppointmentResultAssembler(
                resultMapper,
                patientRepository,
                userRepository,
                historyAssembler
        );
    }

    private User createUser(UUID id, String username, String fullName) {
        return User.restore(
                id, username, "hash", fullName, username + "@hospital.vn", "0900000000",
                UUID.randomUUID(), true, null, now
        );
    }

    private Patient createPatient(UUID id, String code, String name, String phone) {
        return Patient.create(
                code, name, LocalDate.of(1990, 1, 1), Gender.MALE, phone,
                "patient@test.vn", "123 Street", "123456789012", null,
                BloodType.O_POSITIVE, "Contact", "Family", "0901234567",
                true, "v1", UUID.randomUUID()
        );
    }

    @Test
    void toResult_nullAppointment_returnsNull() {
        assertNull(assembler.toResult(null));
    }

    @Test
    void toResultPage_nullPage_returnsEmptyPage() {
        Page<AppointmentResult> result = assembler.toResultPage(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toResults_emptyList_returnsEmptyListWithoutDbCalls() {
        List<AppointmentResult> result = assembler.toResults(List.of(), true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(patientRepository, never()).findAllById(any());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void toResults_batchesLookupsAndEliminatesNPlusOne() {
        UUID apt1Id = UUID.randomUUID();
        UUID apt2Id = UUID.randomUUID();

        UUID patient1Id = UUID.randomUUID();
        UUID patient2Id = UUID.randomUUID();

        UUID doctor1Id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"); // seeded doctor1
        UUID doctor2Id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3"); // seeded doctor2

        Appointment apt1 = Appointment.restore(
                apt1Id, "APT-001", patient1Id, doctor1Id,
                now.plusSeconds(3600), now.plusSeconds(5400),
                AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, UUID.randomUUID(), now
        );

        Appointment apt2 = Appointment.restore(
                apt2Id, "APT-002", patient2Id, doctor2Id,
                now.plusSeconds(7200), now.plusSeconds(9000),
                AppointmentStatus.CONFIRMED, "Khám chuyên khoa",
                null, null, null, UUID.randomUUID(), now
        );

        Patient patient1 = mock(Patient.class);
        when(patient1.getId()).thenReturn(patient1Id);
        when(patient1.getFullName()).thenReturn("Nguyễn Văn An");
        when(patient1.getPatientCode()).thenReturn("BN001");
        when(patient1.getPhone()).thenReturn("0912345678");

        Patient patient2 = mock(Patient.class);
        when(patient2.getId()).thenReturn(patient2Id);
        when(patient2.getFullName()).thenReturn("Trần Thị Bình");
        when(patient2.getPatientCode()).thenReturn("BN002");
        when(patient2.getPhone()).thenReturn("0987654321");

        User doc1 = createUser(doctor1Id, "doctor1", "BS. Nguyễn Văn Nhất");
        User doc2 = createUser(doctor2Id, "doctor2", "BS. Trần Văn Nhị");

        when(patientRepository.findAllById(patientIdsCaptor.capture())).thenReturn(List.of(patient1, patient2));
        when(userRepository.findAllById(userIdsCaptor.capture())).thenReturn(List.of(doc1, doc2));

        List<AppointmentResult> results = assembler.toResults(List.of(apt1, apt2), false);

        assertEquals(2, results.size());

        // Batch querying verified: exactly 1 call each
        verify(patientRepository, times(1)).findAllById(any());
        verify(userRepository, times(1)).findAllById(any());

        Set<UUID> capturedPatientIds = patientIdsCaptor.getValue();
        assertTrue(capturedPatientIds.contains(patient1Id));
        assertTrue(capturedPatientIds.contains(patient2Id));

        AppointmentResult res1 = results.get(0);
        assertEquals("APT-001", res1.appointmentCode());
        assertEquals("Nguyễn Văn An", res1.patientName());
        assertEquals("BN001", res1.patientCode());
        assertEquals("0912345678", res1.patientPhone());
        assertEquals("BS. Nguyễn Văn Nhất", res1.doctorName());
        assertEquals("Nội khoa", res1.department());

        AppointmentResult res2 = results.get(1);
        assertEquals("APT-002", res2.appointmentCode());
        assertEquals("Trần Thị Bình", res2.patientName());
        assertEquals("BN002", res2.patientCode());
        assertEquals("0987654321", res2.patientPhone());
        assertEquals("BS. Trần Văn Nhị", res2.doctorName());
        assertEquals("Ngoại khoa", res2.department());
    }

    @Test
    void resolveDepartment_handlesKnownAndUnknownDoctors() {
        UUID doc1Id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
        UUID doc2Id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");
        UUID unknownDocId = UUID.randomUUID();

        User doc1 = createUser(doc1Id, "doctor1", "BS 1");
        User doc2 = createUser(doc2Id, "doctor2", "BS 2");
        User docUnknown = createUser(unknownDocId, "doctor99", "BS 99");

        assertEquals("Nội khoa", assembler.resolveDepartment(doc1Id, doc1));
        assertEquals("Ngoại khoa", assembler.resolveDepartment(doc2Id, doc2));
        assertEquals("Nội khoa", assembler.resolveDepartment(unknownDocId, docUnknown));
        assertEquals("Nội khoa", assembler.resolveDepartment(null, null));
    }
}

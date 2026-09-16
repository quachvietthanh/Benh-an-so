package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

class GetPatientServiceTest {

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final PatientResultMapper patientResultMapper = mock(PatientResultMapper.class);
    private final GetPatientService service = new GetPatientService(patientRepository, patientResultMapper);

    @Test
    void getsPatientById() {
        UUID patientId = UUID.randomUUID();
        Patient patient = mock(Patient.class);
        PatientResult expected = mock(PatientResult.class);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientResultMapper.toResult(patient)).thenReturn(expected);

        assertEquals(expected, service.getById(patientId));
    }

    @Test
    void getsPatientByCode() {
        Patient patient = mock(Patient.class);
        PatientResult expected = mock(PatientResult.class);
        when(patientRepository.findByPatientCode("BN000001")).thenReturn(Optional.of(patient));
        when(patientResultMapper.toResult(patient)).thenReturn(expected);

        assertEquals(expected, service.getByCode("BN000001"));
    }

    @Test
    void throwsNotFoundWhenPatientCodeDoesNotExist() {
        when(patientRepository.findByPatientCode("BN999999")).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.getByCode("BN999999"));
    }
}

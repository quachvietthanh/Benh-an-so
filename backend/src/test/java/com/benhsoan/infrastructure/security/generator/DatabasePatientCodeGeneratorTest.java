package com.benhsoan.infrastructure.security.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

@ExtendWith(MockitoExtension.class)
class DatabasePatientCodeGeneratorTest {

    @Mock private PatientRepository patientRepository;

    @Test
    void generatesFirstCodeWhenNoPatientExists() {
        when(patientRepository.findTopByOrderByPatientCodeDesc()).thenReturn(Optional.empty());

        assertEquals("BN000001", new DatabasePatientCodeGenerator(patientRepository).generate());
    }

    @Test
    void incrementsTheHighestSequence() {
        Patient patient = patientWithCode("BN000009");
        when(patientRepository.findTopByOrderByPatientCodeDesc()).thenReturn(Optional.of(patient));

        assertEquals("BN000010", new DatabasePatientCodeGenerator(patientRepository).generate());
    }

    @Test
    void fallsBackWhenPatientCodeIsNull() {
        Patient patient = patientWithCode(null);
        when(patientRepository.findTopByOrderByPatientCodeDesc()).thenReturn(Optional.of(patient));

        assertEquals("BN000001", new DatabasePatientCodeGenerator(patientRepository).generate());
    }

    @Test
    void fallsBackWhenPatientCodeIsBlank() {
        Patient patient = patientWithCode("   ");
        when(patientRepository.findTopByOrderByPatientCodeDesc()).thenReturn(Optional.of(patient));

        assertEquals("BN000001", new DatabasePatientCodeGenerator(patientRepository).generate());
    }

    @Test
    void fallsBackWhenPatientCodeDoesNotMatchPattern() {
        Patient patient = patientWithCode("PATIENT-001");
        when(patientRepository.findTopByOrderByPatientCodeDesc()).thenReturn(Optional.of(patient));

        assertEquals("BN000001", new DatabasePatientCodeGenerator(patientRepository).generate());
    }

    @Test
    void fallsBackWhenNumericSuffixOverflows() {
        Patient patient = patientWithCode("BN999999999999999999999999");
        when(patientRepository.findTopByOrderByPatientCodeDesc()).thenReturn(Optional.of(patient));

        assertEquals("BN000001", new DatabasePatientCodeGenerator(patientRepository).generate());
    }

    @Test
    void skipsAlreadyExistingCandidateCodes() {
        Patient patient = patientWithCode("BN000009");
        when(patientRepository.findTopByOrderByPatientCodeDesc()).thenReturn(Optional.of(patient));
        when(patientRepository.existsByPatientCode("BN000010")).thenReturn(true);
        when(patientRepository.existsByPatientCode("BN000011")).thenReturn(false);

        assertEquals("BN000011", new DatabasePatientCodeGenerator(patientRepository).generate());
    }

    private Patient patientWithCode(String code) {
        Patient patient = mock(Patient.class);
        when(patient.getPatientCode()).thenReturn(code);
        return patient;
    }
}
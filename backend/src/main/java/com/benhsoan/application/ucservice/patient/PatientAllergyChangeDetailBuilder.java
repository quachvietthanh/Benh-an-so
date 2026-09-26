package com.benhsoan.application.ucservice.patient;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientAllergy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PatientAllergyChangeDetailBuilder {

    private final ObjectMapper objectMapper;

    public String buildSnapshot(PatientAllergy allergy) {
        if (allergy == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("patientId", allergy.getPatientId() != null ? allergy.getPatientId().toString() : null);
        map.put("allergenType", allergy.getAllergenType());
        map.put("allergenName", allergy.getAllergenName());
        map.put("severity", allergy.getSeverity() != null ? allergy.getSeverity().name() : null);
        map.put("reaction", allergy.getReaction());
        map.put("notes", allergy.getNotes());
        map.put("active", allergy.isActive());

        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"allergenName\":\"" + allergy.getAllergenName() + "\"}";
        }
    }
}

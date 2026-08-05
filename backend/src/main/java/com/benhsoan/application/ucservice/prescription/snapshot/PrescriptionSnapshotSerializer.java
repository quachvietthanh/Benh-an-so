package com.benhsoan.application.ucservice.prescription.snapshot;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrescriptionSnapshotSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(PrescriptionSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize prescription snapshot.",
                    exception
            );
        }
    }
}

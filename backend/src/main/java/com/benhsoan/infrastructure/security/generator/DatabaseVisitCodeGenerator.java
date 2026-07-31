package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.outbound.generator.VisitCodeGenerator;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseVisitCodeGenerator implements VisitCodeGenerator {

    private static final String PREFIX = "VIS";

    private final VisitRepository visitRepository;

    @Override
    public String generate() {
        return visitRepository.findTopByOrderByVisitCodeDesc()
                .map(Visit::getVisitCode)
                .map(this::nextCode)
                .orElse(PREFIX + "000001");
    }

    private String nextCode(String currentCode) {
        int number = Integer.parseInt(currentCode.substring(PREFIX.length()));
        return PREFIX + String.format("%06d", number + 1);
    }
}

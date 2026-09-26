package com.benhsoan.port.dto.command.appointment;

import org.springframework.data.domain.Pageable;

import lombok.Builder;

@Builder
public record GetOverdueAppointmentsCommand(Pageable pageable) {}

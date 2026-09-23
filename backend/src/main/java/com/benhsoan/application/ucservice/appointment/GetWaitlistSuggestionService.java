package com.benhsoan.application.ucservice.appointment;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.query.appointment.GetWaitlistSuggestionQuery;
import com.benhsoan.port.dto.result.appointment.WaitlistSuggestionResult;
import com.benhsoan.port.inbound.appointment.GetWaitlistSuggestionUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetWaitlistSuggestionService implements GetWaitlistSuggestionUseCase {

    private final AppointmentWaitlistRepository appointmentWaitlistRepository;
    private final AppointmentWaitlistResultMapper resultMapper;

    @Override
    public Optional<WaitlistSuggestionResult> getSuggestion(GetWaitlistSuggestionQuery query) {
        Objects.requireNonNull(query, "Query không được để trống");
        Objects.requireNonNull(query.doctorId(), "ID bác sĩ không được để trống");
        Objects.requireNonNull(query.desiredDate(), "Ngày khám không được để trống");

        return appointmentWaitlistRepository.findFirstWaitingByDoctorAndDate(
                query.doctorId(),
                query.desiredDate()
        ).map(resultMapper::toSuggestionResult);
    }
}

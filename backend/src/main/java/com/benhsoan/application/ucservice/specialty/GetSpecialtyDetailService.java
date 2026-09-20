package com.benhsoan.application.ucservice.specialty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.port.dto.result.RoomResult;
import com.benhsoan.port.dto.result.specialty.SpecialtyDetailResult;
import com.benhsoan.port.inbound.specialty.GetSpecialtyDetailUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.repository.specialty.DoctorSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.RoomSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSpecialtyDetailService implements GetSpecialtyDetailUseCase {

    private final SpecialtyRepository specialtyRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final RoomSpecialtyRepository roomSpecialtyRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MedicalRecordTemplateRepository medicalRecordTemplateRepository;

    @Override
    public SpecialtyDetailResult getById(UUID id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new SpecialtyNotFoundException(id));

        List<UUID> doctorIds = doctorSpecialtyRepository.findDoctorIdsBySpecialtyId(id);
        List<SpecialtyDetailResult.AssignedDoctorInfo> doctors = new ArrayList<>();
        if (!doctorIds.isEmpty()) {
            doctors = userRepository.findAllById(doctorIds).stream()
                    .map(u -> SpecialtyDetailResult.AssignedDoctorInfo.builder()
                            .id(u.getId())
                            .username(u.getUsername())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .phone(u.getPhone())
                            .build())
                    .toList();
        }

        List<UUID> roomIds = roomSpecialtyRepository.findRoomIdsBySpecialtyId(id);
        List<RoomResult> rooms = new ArrayList<>();
        if (!roomIds.isEmpty()) {
            rooms = roomIds.stream()
                    .map(roomRepository::findById)
                    .flatMap(java.util.Optional::stream)
                    .map(r -> new RoomResult(r.getId(), r.getCode(), r.getName(), r.isActive(), r.getCreatedAt(), r.getUpdatedAt()))
                    .toList();
        }

        long activeTemplateCount = medicalRecordTemplateRepository.findBySpecialtyIdAndActive(id, true).size();

        return SpecialtyDetailResult.builder()
                .id(specialty.getId())
                .code(specialty.getCode())
                .name(specialty.getName())
                .description(specialty.getDescription())
                .active(specialty.isActive())
                .doctors(doctors)
                .rooms(rooms)
                .activeTemplateCount(activeTemplateCount)
                .createdAt(specialty.getCreatedAt())
                .updatedAt(specialty.getUpdatedAt())
                .build();
    }
}

package com.benhsoan.application.ucservice.specialty;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyCodeAlreadyExistsException;
import com.benhsoan.domain.specialty.exception.SpecialtyNameAlreadyExistsException;
import com.benhsoan.port.dto.command.specialty.CreateSpecialtyCommand;
import com.benhsoan.port.dto.result.specialty.SpecialtyDetailResult;
import com.benhsoan.port.inbound.specialty.CreateSpecialtyUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.repository.specialty.DoctorSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.RoomSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateSpecialtyService implements CreateSpecialtyUseCase {

    private final SpecialtyRepository specialtyRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final RoomSpecialtyRepository roomSpecialtyRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ClockPort clockPort;
    private final CurrentUserPort currentUserPort;
    private final SpecialtyAuditService specialtyAuditService;
    private final GetSpecialtyDetailService getSpecialtyDetailService;

    @Override
    public SpecialtyDetailResult create(CreateSpecialtyCommand command) {
        if (command == null) {
            throw new ValidationException("Create specialty command must not be null.");
        }

        String normalizedCode = command.code() == null ? "" : command.code().trim().toUpperCase(Locale.ROOT);
        if (specialtyRepository.existsByCode(normalizedCode)) {
            throw new SpecialtyCodeAlreadyExistsException(normalizedCode);
        }

        String nameKey = Specialty.toNameKey(command.name());
        if (specialtyRepository.existsByNameKey(nameKey)) {
            throw new SpecialtyNameAlreadyExistsException(command.name());
        }

        validateDoctors(command.doctorIds());
        validateRooms(command.roomIds());

        Instant now = clockPort.now();
        Specialty specialty = Specialty.create(command.code(), command.name(), command.description(), now);
        Specialty saved = specialtyRepository.save(specialty);

        if (command.doctorIds() != null && !command.doctorIds().isEmpty()) {
            doctorSpecialtyRepository.assignDoctors(
                    saved.getId(),
                    command.doctorIds(),
                    currentUserPort.getCurrentUserId(),
                    now
            );
        }

        if (command.roomIds() != null && !command.roomIds().isEmpty()) {
            roomSpecialtyRepository.assignRooms(
                    saved.getId(),
                    command.roomIds(),
                    now
            );
        }

        specialtyAuditService.record(ActionType.CREATE, saved, command.doctorIds(), command.roomIds());

        return getSpecialtyDetailService.getById(saved.getId());
    }

    private void validateDoctors(List<UUID> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) {
            return;
        }
        List<User> users = userRepository.findAllById(doctorIds);
        Set<UUID> foundIds = users.stream().map(User::getId).collect(Collectors.toSet());
        for (UUID doctorId : doctorIds) {
            if (!foundIds.contains(doctorId)) {
                throw new ValidationException("Doctor with id " + doctorId + " does not exist.");
            }
        }
        for (User user : users) {
            if (!RoleConstants.DOCTOR.equals(user.getRoleId())) {
                throw new ValidationException("User " + user.getUsername() + " is not a doctor.");
            }
            if (!user.isActive()) {
                throw new ValidationException("Doctor " + user.getUsername() + " is inactive.");
            }
        }
    }

    private void validateRooms(List<UUID> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return;
        }
        for (UUID roomId : roomIds) {
            if (roomRepository.findActiveById(roomId).isEmpty()) {
                throw new ValidationException("Room with id " + roomId + " does not exist or is inactive.");
            }
        }
    }
}

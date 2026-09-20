package com.benhsoan.application.ucservice.user;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.GetDoctorsUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.specialty.DoctorSpecialtyRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetDoctorsService implements GetDoctorsUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserResultMapper userResultMapper;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;

    @Override
    public List<UserResult> getAllActiveDoctors() {
        var doctorRole = roleRepository.findById(RoleConstants.DOCTOR)
                .orElseThrow(RoleNotFoundException::new);

        return userRepository.findAllActiveByRoleId(RoleConstants.DOCTOR)
                .stream()
                .map(user -> userResultMapper.toResult(user, doctorRole))
                .toList();
    }

    @Override
    public List<UserResult> getActiveDoctorsBySpecialty(UUID specialtyId) {
        if (specialtyId == null) {
            return getAllActiveDoctors();
        }

        var doctorRole = roleRepository.findById(RoleConstants.DOCTOR)
                .orElseThrow(RoleNotFoundException::new);

        List<UUID> doctorIds = doctorSpecialtyRepository.findDoctorIdsBySpecialtyId(specialtyId);
        if (doctorIds.isEmpty()) {
            return List.of();
        }

        return userRepository.findAllById(doctorIds).stream()
                .filter(User::isActive)
                .filter(u -> RoleConstants.DOCTOR.equals(u.getRoleId()))
                .map(user -> userResultMapper.toResult(user, doctorRole))
                .toList();
    }
}

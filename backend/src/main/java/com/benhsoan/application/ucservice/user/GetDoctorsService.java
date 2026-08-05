package com.benhsoan.application.ucservice.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.GetDoctorsUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetDoctorsService implements GetDoctorsUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserResultMapper userResultMapper;

    @Override
    public List<UserResult> getAllActiveDoctors() {

        var doctorRole = roleRepository.findById(RoleConstants.DOCTOR)
                .orElseThrow(RoleNotFoundException::new);

        return userRepository.findAllActiveByRoleId(RoleConstants.DOCTOR)
                .stream()
                .map(user -> userResultMapper.toResult(user, doctorRole))
                .toList();
    }
}

package com.benhsoan.application.ucservice.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.GetUserUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetUserService implements GetUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserResultMapper userResultMapper;

    @Override
    public UserResult getById(UUID id) {

        User user = userRepository.findById(id)
        .orElseThrow(UserNotFoundException::new);

        Role role = roleRepository.findById(user.getRoleId())
        .orElseThrow(RoleNotFoundException::new);

        return userResultMapper.toResult( user, role );
    }
}
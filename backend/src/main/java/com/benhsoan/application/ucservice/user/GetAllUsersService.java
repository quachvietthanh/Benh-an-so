package com.benhsoan.application.ucservice.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.GetAllUsersUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetAllUsersService implements GetAllUsersUseCase {

    private final UserRepository userRepository;
    private final UserResultMapper userResultMapper;
    private final RoleRepository roleRepository;

    @Override
    public List<UserResult> getAll() {

        return userRepository.findAll()
            .stream()
            .map(user -> {

                Role role = roleRepository.findById(user.getRoleId())
                        .orElseThrow(RoleNotFoundException::new);

                return userResultMapper.toResult(
                        user,
                        role
                );
            })
            .toList();

}
}
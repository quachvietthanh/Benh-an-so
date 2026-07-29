package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.UserRestMapper;
import com.benhsoan.adapter.inbound.rest.request.user.CreateUserRequest;
import com.benhsoan.adapter.inbound.rest.request.user.UpdateUserRequest;
import com.benhsoan.adapter.inbound.rest.response.user.UserResponse;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.ActivateUserUseCase;
import com.benhsoan.port.inbound.user.CreateUserUseCase;
import com.benhsoan.port.inbound.user.DeactivateUserUseCase;
import com.benhsoan.port.inbound.user.GetAllUsersUseCase;
import com.benhsoan.port.inbound.user.GetDoctorsUseCase;
import com.benhsoan.port.inbound.user.GetUserUseCase;
import com.benhsoan.port.inbound.user.UpdateUserUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetAllUsersUseCase getUsersUseCase;
    private final GetDoctorsUseCase getDoctorsUseCase;
    private final GetUserUseCase getUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ActivateUserUseCase activateUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;

    private final UserRestMapper userRestMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(
            @Valid @RequestBody CreateUserRequest request
    ) {

        UserResult result =
                createUserUseCase.createUser(
                        userRestMapper.toCommand(request));

        return userRestMapper.toResponse(result);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAll() {

        return userRestMapper.toResponse(
                getUsersUseCase.getAll());
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public List<UserResponse> getDoctors() {

        return userRestMapper.toResponse(
                getDoctorsUseCase.getAllActiveDoctors());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getById(
            @PathVariable UUID id
    ) {

        return userRestMapper.toResponse(
                getUserUseCase.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        UserResult result =
                updateUserUseCase.update(
                        id,
                        userRestMapper.toCommand(request));

        return userRestMapper.toResponse(result);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse activate(
            @PathVariable UUID id
    ) {

        return userRestMapper.toResponse(
                activateUserUseCase.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivate(
            @PathVariable UUID id
    ) {

        return userRestMapper.toResponse(
                deactivateUserUseCase.deactivate(id));
    }
}

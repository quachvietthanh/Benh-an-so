package com.benhsoan.port.inbound.user;

import java.util.UUID;

import com.benhsoan.port.dto.result.UserResult;

public interface ActivateUserUseCase {

    UserResult activate(UUID id);

}
package com.benhsoan.port.inbound.user;

import java.util.UUID;

import com.benhsoan.port.dto.command.user.UpdateUserCommand;
import com.benhsoan.port.dto.result.UserResult;

public interface UpdateUserUseCase {

    UserResult update( UUID id, UpdateUserCommand command );

}
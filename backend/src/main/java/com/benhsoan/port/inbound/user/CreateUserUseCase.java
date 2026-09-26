package com.benhsoan.port.inbound.user;

import com.benhsoan.port.dto.command.user.CreateUserCommand;
import com.benhsoan.port.dto.result.UserResult;

public interface CreateUserUseCase {

    UserResult createUser(
            CreateUserCommand command
    );

}
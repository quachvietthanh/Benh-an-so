package com.benhsoan.port.inbound.user;

import java.util.List;

import com.benhsoan.port.dto.result.UserResult;

public interface GetAllUsersUseCase {

    List<UserResult> getAll();

}
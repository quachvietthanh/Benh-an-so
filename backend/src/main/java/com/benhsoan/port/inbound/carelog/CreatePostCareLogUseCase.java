package com.benhsoan.port.inbound.carelog;

import com.benhsoan.port.dto.command.carelog.CreatePostCareLogCommand;
import com.benhsoan.port.dto.result.PostCareLogResult;

public interface CreatePostCareLogUseCase {

    PostCareLogResult create(CreatePostCareLogCommand command);
}

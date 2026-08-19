package com.benhsoan.port.inbound.carelog;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.carelog.SearchPostCareLogsQuery;
import com.benhsoan.port.dto.result.PostCareLogResult;

public interface SearchPostCareLogsUseCase {

    Page<PostCareLogResult> search(SearchPostCareLogsQuery query);
}

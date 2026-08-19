package com.benhsoan.application.ucservice.carelog;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.carelog.SearchPostCareLogsQuery;
import com.benhsoan.port.dto.result.PostCareLogResult;
import com.benhsoan.port.inbound.carelog.SearchPostCareLogsUseCase;
import com.benhsoan.port.outbound.repository.carelog.PostCareLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchPostCareLogsService implements SearchPostCareLogsUseCase {

    private final PostCareLogRepository postCareLogRepository;
    private final PostCareLogResultMapper resultMapper;
    private final PostCareLogAuthorizer authorizer;

    @Override
    public Page<PostCareLogResult> search(SearchPostCareLogsQuery query) {
        authorizer.requireStaffOrAdmin();

        if (query == null) {
            throw new ValidationException("Query is required.");
        }
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new ValidationException("from must be before or equal to to.");
        }

        return postCareLogRepository.search(query).map(resultMapper::toResult);
    }
}

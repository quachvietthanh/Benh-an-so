package com.benhsoan.port.outbound.repository.carelog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.benhsoan.domain.carelog.PostCareLog;
import com.benhsoan.port.dto.command.carelog.SearchPostCareLogsQuery;

public interface PostCareLogRepository {

    PostCareLog save(PostCareLog careLog);

    List<PostCareLog> findByPatientIdOrderByContactedAtDesc(UUID patientId);

    Page<PostCareLog> search(SearchPostCareLogsQuery query);
}

package com.benhsoan.application.ucservice.queue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.queue.SearchRoomsQuery;
import com.benhsoan.port.dto.result.RoomResult;
import com.benhsoan.port.inbound.queue.SearchRoomsUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.queue.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchRoomsService implements SearchRoomsUseCase {

    private final RoomRepository roomRepository;
    private final RoomAuthorizationService authorizationService;
    private final RoomResultMapper resultMapper;

    @Override
    public Page<RoomResult> search(SearchRoomsQuery query) {
        authorizationService.requireReadAccess();
        var pageable = PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.ASC, "code"));
        return roomRepository.search(query.keyword(), query.active(), pageable).map(resultMapper::toResult);
    }
}

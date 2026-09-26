package com.benhsoan.port.inbound.queue;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.queue.SearchRoomsQuery;
import com.benhsoan.port.dto.result.RoomResult;

public interface SearchRoomsUseCase {

    Page<RoomResult> search(SearchRoomsQuery query);
}

package com.benhsoan.port.outbound.repository.visit;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.visit.VisitHandover;

public interface VisitHandoverRepository {

    VisitHandover save(VisitHandover handover);

    List<VisitHandover> findByVisitId(UUID visitId);
}

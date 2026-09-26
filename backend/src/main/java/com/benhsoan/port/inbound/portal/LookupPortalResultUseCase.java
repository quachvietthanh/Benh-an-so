package com.benhsoan.port.inbound.portal;

import com.benhsoan.port.dto.query.portal.LookupPortalResultQuery;
import com.benhsoan.port.dto.result.portal.PortalLookupResult;

public interface LookupPortalResultUseCase {

    PortalLookupResult lookup(LookupPortalResultQuery query);
}

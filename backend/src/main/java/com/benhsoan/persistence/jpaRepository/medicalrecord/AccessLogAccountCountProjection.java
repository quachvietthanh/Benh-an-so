package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.UUID;

public interface AccessLogAccountCountProjection {

    UUID getAccessedBy();

    long getAccessCount();
}

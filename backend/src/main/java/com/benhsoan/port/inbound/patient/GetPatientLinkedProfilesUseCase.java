package com.benhsoan.port.inbound.patient;

import java.util.List;

import com.benhsoan.port.dto.result.patient.LinkedPatientProfileResult;

public interface GetPatientLinkedProfilesUseCase {

    /**
     * NCL-14-CN-010 CV-02: the authenticated account's own profile first, then every
     * dependent patient whose {@code guardian_user_id} points to that account, ordered by
     * full name ascending.
     */
    List<LinkedPatientProfileResult> getLinkedProfiles();

}

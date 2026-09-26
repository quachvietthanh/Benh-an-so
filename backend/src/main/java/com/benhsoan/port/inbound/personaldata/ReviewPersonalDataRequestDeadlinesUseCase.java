package com.benhsoan.port.inbound.personaldata;

import java.util.List;

import com.benhsoan.port.dto.result.personaldata.PersonalDataRequestResult;

public interface ReviewPersonalDataRequestDeadlinesUseCase {

    List<PersonalDataRequestResult> reviewDueRequests();
}

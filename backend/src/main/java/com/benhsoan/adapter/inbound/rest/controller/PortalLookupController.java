package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.response.portal.PortalLookupResponse;
import com.benhsoan.port.dto.query.portal.LookupPortalResultQuery;
import com.benhsoan.port.inbound.portal.LookupPortalResultUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
public class PortalLookupController {

    private final LookupPortalResultUseCase lookupPortalResultUseCase;

    @GetMapping("/lookup")
    public PortalLookupResponse lookup(
            @RequestParam String code,
            @RequestParam(required = false) String phone
    ) {
        return PortalLookupResponse.from(
                lookupPortalResultUseCase.lookup(new LookupPortalResultQuery(code, phone))
        );
    }
}

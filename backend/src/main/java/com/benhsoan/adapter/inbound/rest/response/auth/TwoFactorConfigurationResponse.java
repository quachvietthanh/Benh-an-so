package com.benhsoan.adapter.inbound.rest.response.auth;

public record TwoFactorConfigurationResponse(

        String roleName,

        boolean twoFactorRequired

) {}

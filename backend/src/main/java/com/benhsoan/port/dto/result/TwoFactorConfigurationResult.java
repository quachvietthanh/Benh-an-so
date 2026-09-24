package com.benhsoan.port.dto.result;

public record TwoFactorConfigurationResult(

        String roleName,

        boolean twoFactorRequired

) {}

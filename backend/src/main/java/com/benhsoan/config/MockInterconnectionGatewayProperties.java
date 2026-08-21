package com.benhsoan.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.benhsoan.infrastructure.interconnection.mock.MockInterconnectionGatewayMode;

@ConfigurationProperties(prefix = "interconnection.mock-gateway")
public record MockInterconnectionGatewayProperties(
        boolean enabled,
        MockInterconnectionGatewayMode mode,
        Duration noResponseDelay
) {
    public MockInterconnectionGatewayProperties {
        mode = mode == null ? MockInterconnectionGatewayMode.ACCEPT : mode;
        noResponseDelay = noResponseDelay == null ? Duration.ofSeconds(15) : noResponseDelay;
    }
}

package com.benhsoan.infrastructure.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.benhsoan.port.inbound.security.ScanAccessLogAnomaliesUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccessLogAnomalyScannerJob {

    private final ScanAccessLogAnomaliesUseCase scanAccessLogAnomaliesUseCase;

    @Scheduled(cron = "0 */5 * * * *")
    public void scan() {
        scanAccessLogAnomaliesUseCase.scan();
    }
}

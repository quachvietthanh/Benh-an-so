package com.benhsoan.infrastructure.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.benhsoan.port.inbound.backup.ExecuteScheduledBackupUseCase;

class ScheduledBackupJobTest {

    private final ExecuteScheduledBackupUseCase useCase = mock(ExecuteScheduledBackupUseCase.class);
    private final ScheduledBackupJob job = new ScheduledBackupJob(useCase);

    @Test
    void scanAndExecuteCallsUseCase() {
        job.scanAndExecute();

        verify(useCase).executeIfDue();
    }

    @Test
    void scanAndExecuteCatchesAndSuppressesExceptions() {
        doThrow(new RuntimeException("Simulated background error")).when(useCase).executeIfDue();

        // Must not throw out of job
        job.scanAndExecute();

        verify(useCase).executeIfDue();
    }
}

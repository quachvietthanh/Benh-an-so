package com.benhsoan.application.ucservice.anonymization;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

/**
 * Runtime read-model / cache for the anonymization mode. It is a lightweight,
 * dependency-free component so the REST mappers (which live in the web slice)
 * can consult the current mode without pulling the persistence/application
 * service graph into {@code @WebMvcTest}.
 *
 * <p>The persistent {@code system_configuration} table is the source of truth;
 * {@link PatientAnonymizationService} loads it on startup and writes it back on
 * every change, keeping this cache in sync.</p>
 */
@Component
public class AnonymizationModeState {

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }
}

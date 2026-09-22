package com.benhsoan.port.inbound.contraindication;

import java.util.UUID;

public interface DeactivateContraindicationRuleUseCase {
    void deactivate(UUID id);
}

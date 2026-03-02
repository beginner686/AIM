package com.ailink.common.enums;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    CREATED,
    SERVICE_FEE_REQUIRED,
    SERVICE_FEE_PAID,
    MATCH_UNLOCKED,
    IN_PROGRESS,
    COMPLETED,
    DISPUTE,
    ARBITRATION,
    CLOSED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            CREATED, Set.of(SERVICE_FEE_REQUIRED, CLOSED),
            SERVICE_FEE_REQUIRED, Set.of(SERVICE_FEE_PAID, CLOSED),
            SERVICE_FEE_PAID, Set.of(MATCH_UNLOCKED, CLOSED),
            MATCH_UNLOCKED, Set.of(IN_PROGRESS, DISPUTE, CLOSED),
            IN_PROGRESS, Set.of(COMPLETED, DISPUTE, CLOSED),
            DISPUTE, Set.of(ARBITRATION, CLOSED),
            ARBITRATION, Set.of(COMPLETED, CLOSED),
            COMPLETED, Set.of(CLOSED),
            CLOSED, Set.of()
    );

    public boolean canTransitTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean canChat() {
        return this == MATCH_UNLOCKED
                || this == IN_PROGRESS
                || this == COMPLETED
                || this == DISPUTE
                || this == ARBITRATION;
    }
}
